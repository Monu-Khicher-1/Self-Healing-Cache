package com.cluster.Master.topology.migration;

import com.cluster.Master.topology.ClusterTopology;
import com.cluster.Master.topology.PartitionRegistry;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.PartitionTransition;
import com.cluster.Master.topology.model.ReplicaSet;
import com.cluster.Master.topology.state.TransitionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link MigrationPlanner}. Diffs the post-join partitions (read from the ring, which now
 * contains the joined node) against the pre-join partitions to find ranges the joined node has
 * become an owner of. For each such range it emits a PLANNED transition (oldSet -> newSet).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMigrationPlanner implements MigrationPlanner {

    private final PartitionRegistry partitionRegistry;
    private final ClusterTopology topology;

    @Override
    public List<Partition> planForJoin(NodeRef joined, List<Partition> prePartitions) {
        long version = topology.version();
        List<Partition> postPartitions = partitionRegistry.partitions();
        List<Partition> plan = new ArrayList<>();

        for (Partition post : postPartitions) {
            ReplicaSet newSet = post.current();
            if (newSet == null || !newSet.owners().contains(joined)) {
                continue; // the joined node did not become an owner of this range
            }
            ReplicaSet oldSet = ownersBeforeJoin(prePartitions, post.rangeStart(), version);
            if (oldSet.owners().equals(newSet.owners())) {
                continue; // ownership unchanged (nothing to migrate)
            }

            PartitionTransition transition =
                    new PartitionTransition(oldSet, newSet, TransitionState.PLANNED, version);
            Partition partition =
                    new Partition(post.id(), post.rangeStart(), post.rangeEnd(), newSet);
            partition.setTransition(transition);
            plan.add(partition);

            log.info("Planned transition for partition {} [{}-{}]: {} -> {}",
                    post.id(), post.rangeStart(), post.rangeEnd(), oldSet.owners(), newSet.owners());
        }
        return plan;
    }

    /** Pre-join owners of the range containing {@code hash}, from the captured pre-partitions. */
    private ReplicaSet ownersBeforeJoin(List<Partition> prePartitions, long hash, long version) {
        for (Partition pre : prePartitions) {
            if (pre.covers(hash)) {
                return pre.current();
            }
        }
        return ReplicaSet.empty();
    }
}
