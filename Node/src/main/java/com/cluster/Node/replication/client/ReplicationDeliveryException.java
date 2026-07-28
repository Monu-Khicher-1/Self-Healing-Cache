package com.cluster.Node.replication.client;

/**
 * Raised when a replication task fails to be delivered to (or is rejected by) a replica.
 * Used by workers to decide whether to retry.
 */
public class ReplicationDeliveryException extends RuntimeException {
    public ReplicationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
