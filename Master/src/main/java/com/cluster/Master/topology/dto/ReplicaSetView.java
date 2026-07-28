package com.cluster.Master.topology.dto;

import java.util.List;

/**
 * Wire representation of a replica set: primary plus ordered replicas and the set version.
 */
public record ReplicaSetView(NodeRefDto primary, List<NodeRefDto> replicas, long version) {
}
