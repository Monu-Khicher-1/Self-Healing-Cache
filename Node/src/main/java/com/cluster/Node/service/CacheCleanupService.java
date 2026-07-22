package com.cluster.Node.service;


import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheCleanupService {
    private final CacheRepository cacheRepository;

    // Scheduled cleanup in 1 minute
    @Scheduled(fixedRate = 60000)
    void cleanup() {
        log.info("Loading cache list from repository");
        List<CacheEntry> data=cacheRepository.getAllValues();
        if(data == null || data.isEmpty()) {
            log.info("No cache found for cleanup");
            return;
        }
        log.info("Starting cleanup.");
        for (CacheEntry entry : data) {
            if(entry.getTtl().before(Timestamp.valueOf(LocalDateTime.now()))) {
                log.info("Removing cache entry: {}", entry);
                cacheRepository.removeValue(entry);
            }
        }
        log.info("Cleanup completed successfully.");
    }
}
