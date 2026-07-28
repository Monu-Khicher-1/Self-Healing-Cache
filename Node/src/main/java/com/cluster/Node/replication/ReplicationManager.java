package com.cluster.Node.replication;

import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.replication.model.ReplicaEndpoint;

import java.util.List;

/**
 * Primary-side entry point for propagating local mutations to replicas. Implementations must
 * be non-blocking: they enqueue work and return immediately so client responses are never
 * delayed by replication.
 */
public interface ReplicationManager {

    /**
     * Asynchronously replicates a stored entry (PUT) to the given replicas.
     */
    void replicatePut(CacheEntry entry, List<ReplicaEndpoint> replicas);

    /**
     * Asynchronously replicates a delete to the given replicas. The {@code version} must be
     * strictly newer than the value being removed so replicas can reject stale deletes.
     */
    void replicateDelete(String key, long version, List<ReplicaEndpoint> replicas);
}
