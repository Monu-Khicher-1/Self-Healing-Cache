package com.cluster.Master.topology.dto;

/**
 * Wire representation of a partition: its ring range, current owners, the incoming owners while
 * a transition is in flight (null otherwise), and the transition state name.
 */
public record PartitionView(String partitionId,
                            long rangeStart,
                            long rangeEnd,
                            ReplicaSetView current,
                            ReplicaSetView incoming,
                            String transitionState) {
}
