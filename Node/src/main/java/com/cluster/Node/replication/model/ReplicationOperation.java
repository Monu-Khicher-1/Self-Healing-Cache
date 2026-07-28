package com.cluster.Node.replication.model;

/**
 * Type of mutation that must be propagated from a primary to its replicas.
 */
public enum ReplicationOperation {
    PUT,
    DELETE
}
