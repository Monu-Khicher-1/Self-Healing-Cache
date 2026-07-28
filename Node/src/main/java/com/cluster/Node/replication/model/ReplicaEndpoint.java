package com.cluster.Node.replication.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Network coordinates of a replica node. Supplied by the master on every write so a
 * primary knows where to fan out its updates without having to query the ring itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplicaEndpoint {
    private String nodeId;
    private String hostname;
    private int port;

    public String baseUrl() {
        return "http://" + hostname + ":" + port;
    }
}
