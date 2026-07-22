package com.cluster.Node.service;



import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.repository.CacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CacheService {
    private final CacheRepository cacheRepository;
    public CacheService(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public CacheEntry setEntry(CacheRequest request) {
        log.info("Processing request: {}",request);
        CacheEntry entry = new CacheEntry(request.getKey(), request.getValue(), Timestamp.valueOf(LocalDateTime.now().plusMinutes(request.getTime())));
        return cacheRepository.put(request.getKey(), entry);
    }

    public CacheEntry getEntry(String key) {
        return cacheRepository.get(key);
    }

    public List<CacheEntry> getAllEntries() {
        return cacheRepository.getAllValues();
    }
}
