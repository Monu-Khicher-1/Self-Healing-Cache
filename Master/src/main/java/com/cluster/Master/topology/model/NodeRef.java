package com.cluster.Master.topology.model;

/**
 * Immutable network identity of a node, decoupled from the richer {@code ClusterNode} registry
 * entity. Topology/replication logic depends only on this small value type, which keeps the
 * domain model free of registry/heartbeat concerns and makes it trivial to serialise to nodes.
 */
public record NodeRef(String id, String host, int port) {

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
