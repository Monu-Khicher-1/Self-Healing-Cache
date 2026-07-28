package com.cluster.Node.replication;

import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.replication.model.ReplicaEndpoint;
import com.cluster.Node.replication.model.ReplicationOperation;
import com.cluster.Node.replication.model.ReplicationRequest;
import com.cluster.Node.replication.model.ReplicationTask;
import com.cluster.Node.replication.queue.ReplicationQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default {@link ReplicationManager}. Turns a local mutation into one {@link ReplicationTask}
 * per replica and hands them to the {@link ReplicationQueue}. It performs no network I/O
 * itself, keeping the write path (and hence the client response) unblocked.
 */
@Slf4j
@Service
public class DefaultReplicationManager implements ReplicationManager {

    private final ReplicationQueue queue;

    public DefaultReplicationManager(ReplicationQueue queue) {
        this.queue = queue;
    }

    @Override
    public void replicatePut(CacheEntry entry, List<ReplicaEndpoint> replicas) {
        if (entry == null) {
            return;
        }
        long expireAt = entry.getExpireAt() == null ? 0L : entry.getExpireAt().getTime();
        ReplicationRequest request = ReplicationRequest.builder()
                .key(entry.getKey())
                .value(entry.getValue())
                .expireAt(expireAt)
                .version(entry.getVersion())
                .operation(ReplicationOperation.PUT)
                .build();
        enqueue(request, replicas);
    }

    @Override
    public void replicateDelete(String key, long version, List<ReplicaEndpoint> replicas) {
        ReplicationRequest request = ReplicationRequest.builder()
                .key(key)
                .version(version)
                .operation(ReplicationOperation.DELETE)
                .build();
        enqueue(request, replicas);
    }

    private void enqueue(ReplicationRequest request, List<ReplicaEndpoint> replicas) {
        if (replicas == null || replicas.isEmpty()) {
            log.debug("No replicas for key {}; skipping replication", request.getKey());
            return;
        }
        for (ReplicaEndpoint replica : replicas) {
            if (replica == null) {
                continue;
            }
            queue.submit(new ReplicationTask(replica, request));
        }
    }
}
