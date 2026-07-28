package com.cluster.Node.topology;

/**
 * Node-side wire mirror of the master's {@code PartitionView}.
 */
public record PartitionView(String partitionId,
                            long rangeStart,
                            long rangeEnd,
                            ReplicaSetView current,
                            ReplicaSetView incoming,
                            String transitionState) {
}
