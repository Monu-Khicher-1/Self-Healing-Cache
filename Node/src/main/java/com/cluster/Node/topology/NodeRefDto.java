package com.cluster.Node.topology;

/**
 * Node-side wire mirror of the master's {@code NodeRefDto}. Field names match verbatim so Jackson
 * round-trips the published snapshot cleanly.
 */
public record NodeRefDto(String id, String host, int port) {
}
