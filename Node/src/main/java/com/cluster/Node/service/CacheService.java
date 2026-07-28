package com.cluster.Node.service;



import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.repository.CacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns the local (primary) view of the cache. Assigns absolute expiry and a monotonic
 * per-key version to every write. Replication is intentionally NOT triggered here so that
 * the repository/service layer stays free of networking concerns; callers (controllers)
 * hand the resulting entry to the {@code ReplicationManager}.
 */
@Slf4j
@Service
public class CacheService {
    private final CacheRepository cacheRepository;
    public CacheService(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public CacheEntry setEntry(CacheRequest request) {
        log.info("Processing request: {}", request);
        Timestamp expireAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(request.getTime()));
        CacheEntry entry = CacheEntry.builder()
                .key(request.getKey())
                .value(request.getValue())
                .expireAt(expireAt)
                .version(nextVersion(request.getKey()))
                .build();
        return cacheRepository.put(request.getKey(), entry);
    }

    public CacheEntry getEntry(String key) {
        return cacheRepository.get(key);
    }

    /**
     * Deletes the key locally and returns the version stamped on the tombstone so the caller
     * can replicate the delete with a strictly-newer version than the value it removed.
     */
    public long deleteEntry(String key) {
        long version = nextVersion(key);
        cacheRepository.removeByKey(key);
        log.info("Deleted key {} with tombstone version {}", key, version);
        return version;
    }

    public List<CacheEntry> getAllEntries() {
        return cacheRepository.getAllValues();
    }

    /**
     * Computes the next version for a key. The primary is the single writer per key
     * (all client writes are routed here by the master), so a simple increment is safe.
     */
    public long nextVersion(String key) {
        CacheEntry existing = cacheRepository.get(key);
        return existing == null ? 1L : existing.getVersion() + 1L;
    }
}
