package com.cluster.Master.topology.dto;

import java.util.List;

/**
 * Authoritative topology snapshot published by the master to every node after any membership
 * change. Carries the current topology version so a node can ignore out-of-order/stale pushes
 * and (in a later phase) so clients can route directly. Analogous to Hazelcast's partition
 * table and Redis Cluster's cluster slots view.
 */
public record TopologySnapshot(long version,
                               List<NodeRefDto> nodes,
                               List<PartitionView> partitions) {
}
