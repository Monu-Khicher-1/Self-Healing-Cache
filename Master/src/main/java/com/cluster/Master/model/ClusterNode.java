package com.cluster.Master.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ClusterNode {
    private String id;
    private String hostname;
    private int port;
    private Timestamp lastHeartbeat;
    // Should keep track of rebalancing
    // New node should have pointer to old node
    // Hashing strategy/Hash Value should be added for distribution

}
