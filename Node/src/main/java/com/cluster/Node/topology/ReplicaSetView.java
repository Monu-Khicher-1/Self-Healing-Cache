package com.cluster.Node.topology;

import java.util.List;

/**
 * Node-side wire mirror of the master's {@code ReplicaSetView}.
 */
public record ReplicaSetView(NodeRefDto primary, List<NodeRefDto> replicas, long version) {
}
