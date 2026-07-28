package com.cluster.Master.topology.migration;

import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;

import java.util.List;

/**
 * Drives the replica-set-transition state machine for topology changes. Kept separate from
 * failover/promotion (req 9): promotion only relabels roles, whereas migration moves data through
 * a PLANNED -> DUAL_WRITE -> COPYING -> VERIFYING -> COMPLETED lifecycle. The coordinator owns that
 * lifecycle and nothing else, communicating with planner/executor/store through interfaces.
 */
public interface MigrationCoordinator {

    /**
     * Reacts to a node joining: plans the affected partition transitions and runs each to
     * completion asynchronously, so client reads/writes continue uninterrupted (req 4/5).
     *
     * @param joined        the node that just joined (already added to the ring)
     * @param prePartitions partitions as they were <em>before</em> the join (captured by the caller)
     */
    void onNodeJoined(NodeRef joined, List<Partition> prePartitions);
}
