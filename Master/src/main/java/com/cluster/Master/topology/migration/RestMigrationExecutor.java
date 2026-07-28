package com.cluster.Master.topology.migration;

import com.cluster.Master.router.DataTransferRouter;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.PartitionTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default {@link MigrationExecutor}. For a partition's transition it seeds every new owner (those
 * in the new replica set but not the old one) from the old primary, using a single node-to-node
 * stream per destination.
 *
 * <p>The old primary is chosen as the source because, during a transition, it remains the write
 * coordinator (see {@link com.cluster.Master.topology.DefaultReplicaMetadataService}) and therefore
 * always holds the authoritative, latest-version copy of every key in the range. Streaming from it
 * fans out data directly to each new destination — the Dynamo/Cassandra bootstrap-streaming model
 * rather than routing bytes through the master.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestMigrationExecutor implements MigrationExecutor {

    private final DataTransferRouter dataTransferRouter;

    @Override
    public int copy(Partition partition) {
        PartitionTransition transition = partition.transition();
        if (transition == null) {
            return 0;
        }
        NodeRef source = transition.oldSet().primary();
        if (source == null) {
            log.warn("Partition {} has no old primary to copy from; skipping bulk copy", partition.id());
            return 0;
        }

        List<NodeRef> oldOwners = transition.oldSet().owners();
        List<NodeRef> newOwners = transition.newSet().owners();

        // Range [rangeStart, rangeEnd] maps to the node's (start, end] matcher as
        // (rangeStart - 1, rangeEnd]; wrap-around (start > end) is handled natively by the node.
        long startHash = partition.rangeStart() - 1;
        long endHash = partition.rangeEnd();

        int seeded = 0;
        for (NodeRef target : newOwners) {
            if (oldOwners.contains(target)) {
                continue; // already holds a copy — nothing to stream
            }
            log.info("Copying partition {} (range ({},{}]) from old primary {} to new owner {}",
                    partition.id(), startHash, endHash, source.id(), target.id());
            dataTransferRouter.notifyTransferKeys(source, target, startHash, endHash);
            seeded++;
        }
        return seeded;
    }
}
