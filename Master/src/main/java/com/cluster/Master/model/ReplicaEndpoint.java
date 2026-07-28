package com.cluster.Master.model;

import com.cluster.Master.topology.model.NodeRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serialisable view of a replica node sent to the primary on each write so it knows where to
 * fan out updates. Field names mirror the node-side {@code ReplicaEndpoint} DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicaEndpoint {
    private String nodeId;
    private String hostname;
    private int port;

    public static ReplicaEndpoint from(ClusterNode node) {
        return ReplicaEndpoint.builder()
                .nodeId(node.getId())
                .hostname(node.getHostname())
                .port(node.getPort())
                .build();
    }

    public static ReplicaEndpoint from(NodeRef node) {
        return ReplicaEndpoint.builder()
                .nodeId(node.id())
                .hostname(node.host())
                .port(node.port())
                .build();
    }
}
