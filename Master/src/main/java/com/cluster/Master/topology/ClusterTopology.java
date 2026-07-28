package com.cluster.Master.topology;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.replication.ReplicationProperties;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.state.NodeState;
import com.cluster.Master.topology.state.StateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single authoritative cluster state: topology version, hash ring, node lifecycle states,
 * replication factor, and in-flight migrations. All membership changes (join/leave/fail/recover)
 * update ring and version atomically under one lock, ensuring they never drift.
 * See {@link TopologyManager} for mutation policy and event publishing.
 */
@Slf4j
@Component
public class ClusterTopology {

    private final HashRing ring;
    private final ReplicationProperties replicationProperties;

    private final AtomicLong version = new AtomicLong(0);
    private final ConcurrentMap<String, StateMachine<NodeState>> nodeStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Partition> migrations = new ConcurrentHashMap<>();

    /** Guards the ring + version + node-state trio so membership changes are atomic together. */
    private final ReentrantLock membershipLock = new ReentrantLock();

    public ClusterTopology(HashRing ring, ReplicationProperties replicationProperties) {
        this.ring = ring;
        this.replicationProperties = replicationProperties;
    }

    // === Membership mutations (each bumps version once) ===

    public long join(ClusterNode node) {
        membershipLock.lock();
        try {
            StateMachine<NodeState> sm =
                    nodeStates.computeIfAbsent(node.getId(), id -> new StateMachine<>(NodeState.REGISTERING));
            if (sm.canTransition(NodeState.ACTIVE)) {
                sm.transition(NodeState.ACTIVE);
            }
            ring.addNode(node);
            return bump("join", node.getId());
        } finally {
            membershipLock.unlock();
        }
    }

    /** Graceful departure: removes from ring, bumps version. */
    public long leave(ClusterNode node) {
        membershipLock.lock();
        try {
            StateMachine<NodeState> sm = nodeStates.get(node.getId());
            if (sm != null && sm.canTransition(NodeState.DEAD)) {
                sm.transition(NodeState.DEAD);
            }
            nodeStates.remove(node.getId());
            ring.removeNode(node.getId());
            return bump("leave", node.getId());
        } finally {
            membershipLock.unlock();
        }
    }

    /**
     * Failure: marks DEAD, removes from ring, retains state for potential recovery.
     */
    public long fail(ClusterNode node) {
        membershipLock.lock();
        try {
            StateMachine<NodeState> sm = nodeStates.get(node.getId());
            if (sm != null && sm.canTransition(NodeState.DEAD)) {
                sm.transition(NodeState.DEAD);
            }
            ring.removeNode(node.getId());
            return bump("fail", node.getId());
        } finally {
            membershipLock.unlock();
        }
    }

    /** Recovery: marks RECOVERING → ACTIVE, re-adds to ring, bumps version. */
    public long recover(ClusterNode node) {
        membershipLock.lock();
        try {
            StateMachine<NodeState> sm =
                    nodeStates.computeIfAbsent(node.getId(), id -> new StateMachine<>(NodeState.REGISTERING));
            if (sm.canTransition(NodeState.RECOVERING)) {
                sm.transition(NodeState.RECOVERING);
            }
            if (sm.canTransition(NodeState.ACTIVE)) {
                sm.transition(NodeState.ACTIVE);
            }
            ring.addNode(node);
            return bump("recover", node.getId());
        } finally {
            membershipLock.unlock();
        }
    }

    private long bump(String reason, String nodeId) {
        long v = version.incrementAndGet();
        log.info("Topology change: {} node {}; version -> {}", reason, nodeId, v);
        return v;
    }

    /**
     * Advances version for non-membership changes (e.g., migration completion, promotion).
     * Membership changes (join/leave/fail/recover) use their own methods.
     */
    public long nextVersion(String reason) {
        long v = version.incrementAndGet();
        log.info("Topology change: {}; version -> {}", reason, v);
        return v;
    }

    // === Reads (single source of truth for cluster state) ===

    public long version() {
        return version.get();
    }

    /** Replication factor (clamped ≥1). */
    public int factor() {
        return Math.max(1, replicationProperties.getFactor());
    }

    /** All distinct physical nodes on the ring. */
    public List<ClusterNode> activeNodes() {
        return ring.getAllNodes();
    }

    public NodeState nodeState(String nodeId) {
        StateMachine<NodeState> sm = nodeStates.get(nodeId);
        return sm == null ? null : sm.current();
    }

    /** The consistent hash ring for read-only placement queries. Mutate only via this class. */
    public HashRing ring() {
        return ring;
    }

    public long keyHash(String key) {
        return ring.keyHash(key);
    }

    // === In-flight migrations (replica-set transitions) ===

    public void putMigration(Partition partition) {
        migrations.put(partition.id(), partition);
    }

    public void removeMigration(String partitionId) {
        migrations.remove(partitionId);
    }

    public boolean isMigrating(String partitionId) {
        Partition partition = migrations.get(partitionId);
        return partition != null && partition.isTransitioning();
    }

    /** The active migration whose range covers {@code keyHash}, if any. */
    public Optional<Partition> migrationForHash(long keyHash) {
        for (Partition partition : migrations.values()) {
            if (partition.isTransitioning() && partition.covers(keyHash)) {
                return Optional.of(partition);
            }
        }
        return Optional.empty();
    }

    public Collection<Partition> migrations() {
        return migrations.values();
    }
}
