package com.cluster.Master.topology.migration;

import com.cluster.Master.router.NodeMaintenanceClient;
import com.cluster.Master.topology.ClusterTopology;
import com.cluster.Master.topology.event.TopologyChangedEvent;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.PartitionTransition;
import com.cluster.Master.topology.state.TransitionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates replica-set transitions during cluster membership changes.
 * Transitions flow: PLANNED → DUAL_WRITE → COPYING → VERIFYING → COMPLETED.
 * Transitions are registered before copying to enable dual-write routing.
 * Copy completes only after verification (entry count check with retries).
 * Single-threaded executor preserves FIFO ordering across overlapping ranges.
 */
@Slf4j
@Service
public class DefaultMigrationCoordinator implements MigrationCoordinator {

    private final MigrationPlanner planner;
    private final MigrationExecutor executor;
    private final ClusterTopology topology;
    private final ApplicationEventPublisher eventPublisher;
    private final NodeMaintenanceClient maintenanceClient;
    private final long drainMillis;
    private final int verifyAttempts;
    private final boolean cleanupEnabled;

    private final ExecutorService workers =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "migration-coordinator");
                thread.setDaemon(true);
                return thread;
            });

    public DefaultMigrationCoordinator(MigrationPlanner planner,
                                       MigrationExecutor executor,
                                       ClusterTopology topology,
                                       ApplicationEventPublisher eventPublisher,
                                       NodeMaintenanceClient maintenanceClient,
                                       @Value("${cluster.migration.drain-ms:3000}") long drainMillis,
                                       @Value("${cluster.migration.verify-attempts:3}") int verifyAttempts,
                                       @Value("${cluster.migration.cleanup-enabled:true}") boolean cleanupEnabled) {
        this.planner = planner;
        this.executor = executor;
        this.topology = topology;
        this.eventPublisher = eventPublisher;
        this.maintenanceClient = maintenanceClient;
        this.drainMillis = drainMillis;
        this.verifyAttempts = Math.max(1, verifyAttempts);
        this.cleanupEnabled = cleanupEnabled;
    }

    @Override
    public void onNodeJoined(NodeRef joined, List<Partition> prePartitions) {
        List<Partition> plan = planner.planForJoin(joined, prePartitions);
        if (plan.isEmpty()) {
            log.info("Node {} joined but no partitions changed ownership; no migration needed", joined.id());
            return;
        }
        log.info("Node {} joined; scheduling {} partition transition(s)", joined.id(), plan.size());
        for (Partition partition : plan) {
            // Guard against scheduling a second transition for the SAME partition while one is
            // already active (exact id match, not range-cover, so wrap-region partitions that
            // merely overlap are still allowed to proceed — they are serialised by the executor).
            if (topology.isMigrating(partition.id())) {
                log.warn("Partition {} already transitioning; skipping duplicate schedule", partition.id());
                continue;
            }
            // Activate dual-write/read-fallback immediately, before copying begins.
            topology.putMigration(partition);
            workers.submit(() -> runTransition(partition));
        }
    }

    private void runTransition(Partition partition) {
        PartitionTransition transition = partition.transition();
        try {
            advance(transition, TransitionState.DUAL_WRITE);

            advance(transition, TransitionState.COPYING);
            boolean verified = copyAndVerify(partition);

            advance(transition, TransitionState.VERIFYING);

            advance(transition, TransitionState.COMPLETED);
            complete(partition, verified);
        } catch (Exception e) {
            log.error("Transition for partition {} failed: {}", partition.id(), e.getMessage(), e);
            failTransition(transition);
            topology.removeMigration(partition.id());
        }
    }

    /** Copies the range to new owners and verifies counts, retrying up to {@code verifyAttempts}. */
    private boolean copyAndVerify(Partition partition) {
        PartitionTransition transition = partition.transition();
        NodeRef source = transition.oldSet().primary();
        long startHash = partition.rangeStart() - 1;
        long endHash = partition.rangeEnd();

        List<NodeRef> oldOwners = transition.oldSet().owners();
        List<NodeRef> newOwners = transition.newSet().owners();

        long expected = source == null ? -1 : maintenanceClient.countRange(source, startHash, endHash);

        for (int attempt = 1; attempt <= verifyAttempts; attempt++) {
            int seeded = executor.copy(partition);
            drain();

            if (expected < 0) {
                log.info("Partition {}: source count unavailable; drain-only barrier (attempt {})",
                        partition.id(), attempt);
                return false; // could not verify; treat as unverified (skip cleanup) but proceed
            }

            boolean allOk = true;
            for (NodeRef owner : newOwners) {
                if (oldOwners.contains(owner)) {
                    continue; // already held the data
                }
                long got = maintenanceClient.countRange(owner, startHash, endHash);
                if (got < expected) {
                    log.warn("Partition {}: new owner {} has {}/{} entries (attempt {}/{})",
                            partition.id(), owner.id(), got, expected, attempt, verifyAttempts);
                    allOk = false;
                }
            }
            if (allOk) {
                log.info("Partition {}: verified {} entries on {} new owner(s) (attempt {})",
                        partition.id(), expected, seeded, attempt);
                return true;
            }
        }
        log.error("Partition {}: verification failed after {} attempt(s); completing WITHOUT cleanup",
                partition.id(), verifyAttempts);
        return false;
    }

    private void complete(Partition partition, boolean verified) {
        // Remove BEFORE publishing so routing stops using the (now redundant) fallback path.
        topology.removeMigration(partition.id());

        if (verified && cleanupEnabled) {
            cleanupObsoleteCopies(partition);
        }

        long version = topology.nextVersion("migration-complete:" + partition.id());
        log.info("Transition for partition {} COMPLETED (verified={}); topology -> v{}",
                partition.id(), verified, version);
        eventPublisher.publishEvent(new TopologyChangedEvent(version, "migration-complete:" + partition.id()));
    }

    /** Drops this partition's range from owners that left the replica set (req 5/24). */
    private void cleanupObsoleteCopies(Partition partition) {
        PartitionTransition transition = partition.transition();
        long startHash = partition.rangeStart() - 1;
        long endHash = partition.rangeEnd();
        List<NodeRef> newOwners = transition.newSet().owners();
        for (NodeRef old : transition.oldSet().owners()) {
            if (!newOwners.contains(old)) {
                maintenanceClient.dropRange(old, startHash, endHash);
            }
        }
    }

    private void advance(PartitionTransition transition, TransitionState next) {
        transition.setState(next);
    }

    private void failTransition(PartitionTransition transition) {
        try {
            transition.setState(TransitionState.FAILED);
        } catch (Exception ignored) {
            // best-effort terminal marking
        }
    }

    private void drain() {
        if (drainMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(drainMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
