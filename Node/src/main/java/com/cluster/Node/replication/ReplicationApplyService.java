package com.cluster.Node.replication;

import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.replication.model.ReplicationRequest;
import com.cluster.Node.repository.CacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/**
 * Replica-side application of incoming replication requests. Enforces last-writer-wins:
 * an update is applied only when its version is strictly newer than what is stored locally,
 * so out-of-order or duplicate deliveries can never overwrite fresher data.
 *
 * <p>This class owns the conflict-resolution policy; {@link CacheRepository} stays a dumb
 * store, keeping replication logic decoupled from storage.
 */
@Slf4j
@Service
public class ReplicationApplyService {

    private final CacheRepository cacheRepository;

    public ReplicationApplyService(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public void apply(ReplicationRequest request) {
        if (request == null || request.getOperation() == null) {
            log.warn("Ignoring malformed replication request: {}", request);
            return;
        }
        switch (request.getOperation()) {
            case PUT -> applyPut(request);
            case DELETE -> applyDelete(request);
        }
    }

    private void applyPut(ReplicationRequest request) {
        CacheEntry existing = cacheRepository.get(request.getKey());
        if (!isNewer(request, existing)) {
            log.debug("Rejecting stale PUT for key {} (incoming v{} <= local v{})",
                    request.getKey(), request.getVersion(),
                    existing == null ? null : existing.getVersion());
            return;
        }
        CacheEntry entry = CacheEntry.builder()
                .key(request.getKey())
                .value(request.getValue())
                .expireAt(new Timestamp(request.getExpireAt()))
                .version(request.getVersion())
                .build();
        cacheRepository.put(entry.getKey(), entry);
        log.info("Applied replicated PUT key={} v{}", entry.getKey(), entry.getVersion());
    }

    private void applyDelete(ReplicationRequest request) {
        CacheEntry existing = cacheRepository.get(request.getKey());
        if (existing == null) {
            log.debug("Replicated DELETE for absent key {}; nothing to do", request.getKey());
            return;
        }
        if (!isNewer(request, existing)) {
            log.debug("Rejecting stale DELETE for key {} (incoming v{} <= local v{})",
                    request.getKey(), request.getVersion(), existing.getVersion());
            return;
        }
        cacheRepository.removeByKey(request.getKey());
        log.info("Applied replicated DELETE key={} v{}", request.getKey(), request.getVersion());
    }

    private boolean isNewer(ReplicationRequest request, CacheEntry existing) {
        return existing == null || request.getVersion() > existing.getVersion();
    }
}
