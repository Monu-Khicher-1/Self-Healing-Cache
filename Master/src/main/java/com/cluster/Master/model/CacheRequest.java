package com.cluster.Master.model;


import lombok.Data;

import java.util.List;

@Data
public class CacheRequest {
    private String key;
    private String value;
    private int time;
    /** Populated by the router before forwarding to the primary node. */
    private List<ReplicaEndpoint> replicas;
}