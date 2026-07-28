package com.cluster.Node.model.cache;

import com.cluster.Node.replication.model.ReplicaEndpoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheRequest {
    private String key;
    private String value;
    private int time;
    /** Replica endpoints supplied by the master; empty/null when the write is not client-originated. */
    private List<ReplicaEndpoint> replicas;

    public CacheRequest(String key, String value, int time) {
        this(key, value, time, null);
    }
}
