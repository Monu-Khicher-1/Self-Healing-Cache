package com.cluster.Node.topology;

import java.util.List;

/**
 * Node-side wire mirror of the master's {@code TopologySnapshot}. The node stores the latest
 * version-stamped snapshot so it can (later) route reads directly and reject stale pushes.
 */
public record TopologySnapshot(long version,
                               List<NodeRefDto> nodes,
                               List<PartitionView> partitions) {
}
