package com.cluster.Node.replication.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A single unit of work for the replication subsystem: deliver one {@link ReplicationRequest}
 * to one {@link ReplicaEndpoint}. The mutable attempt counter lets workers implement bounded
 * retries without allocating a new task per attempt.
 */
@Getter
@RequiredArgsConstructor
public class ReplicationTask {
    private final ReplicaEndpoint target;
    private final ReplicationRequest request;
    private int attempts;

    public int incrementAttempts() {
        return ++attempts;
    }
}
