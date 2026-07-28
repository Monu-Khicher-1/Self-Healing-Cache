package com.cluster.Node.replication.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable payload sent over the internal replication REST API. Carries an absolute
 * {@code expireAt} (epoch millis) instead of a remaining TTL so replicas never drift,
 * plus the {@code version} used by replicas to reject stale updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicationRequest {
    private String key;
    private String value;
    private long expireAt;
    private long version;
    private ReplicationOperation operation;
}
