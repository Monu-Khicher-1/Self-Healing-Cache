package com.cluster.Master.topology.dto;

/**
 * Wire representation of a {@link com.cluster.Master.topology.model.NodeRef}. Field names are
 * shared verbatim with the node-side DTO so Jackson round-trips cleanly.
 */
public record NodeRefDto(String id, String host, int port) {
}
