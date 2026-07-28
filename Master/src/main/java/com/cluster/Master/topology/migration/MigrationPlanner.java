package com.cluster.Master.topology.migration;

import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;

import java.util.List;

/**
 * Computes the set of {@link Partition} transitions required when a node joins, <em>without</em>
 * moving any data (req 4: "generate a migration plan instead of immediately moving keys"). Each
 * returned partition carries a PLANNED {@link com.cluster.Master.topology.model.PartitionTransition}
 * describing its old replica set (pre-join owners) and new replica set (post-join owners).
 *
 * <p>Only ranges whose ownership actually changes are planned, giving the minimal key movement that
 * consistent hashing is prized for — the same property Dynamo/Cassandra rely on.
 */
public interface MigrationPlanner {

    /**
     * @param joined         the node that just joined
     * @param prePartitions  partitions as they were <em>before</em> the node was added to the ring
     * @return the partitions that must transition, each with a PLANNED transition attached
     */
    List<Partition> planForJoin(NodeRef joined, List<Partition> prePartitions);
}
