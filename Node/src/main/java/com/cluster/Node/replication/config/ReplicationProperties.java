package com.cluster.Node.replication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the primary-side replication pipeline. Bound from {@code cluster.replication.*}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cluster.replication")
public class ReplicationProperties {
    /** Number of dedicated background worker threads draining the replication queue. */
    private int workers = 2;
    /** Total delivery attempts per task before it is marked failed. */
    private int maxAttempts = 3;
    /** Base backoff between retries; grows linearly with the attempt number. */
    private long retryBackoffMs = 200;
    /** Bounded capacity of the in-memory replication queue. */
    private int queueCapacity = 10000;
}
