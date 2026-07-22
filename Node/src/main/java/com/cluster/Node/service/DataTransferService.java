package com.cluster.Node.service;


import com.cluster.Node.model.DataTransferRequest;
import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataTransferService {
    private final RestClient restClient;
    private final CacheRepository cacheRepository;

    @Async
    public void transferData(DataTransferRequest request) {
        log.info("Starting data transfer for hash range [{}, {}] to URL: {}",
                 request.getStartHash(), request.getEndHash(), request.getUrl());

        try {
            // Get all cache entries within the specified hash range
            List<CacheEntry> entriesToTransfer = cacheRepository.getEntriesByHashRange(
                    request.getStartHash(),
                    request.getEndHash()
            );

            if (entriesToTransfer.isEmpty()) {
                log.info("No cache entries found in the specified hash range");
                return;
            }

            log.info("Found {} cache entries to transfer", entriesToTransfer.size());
            List<CacheRequest> dataToSend = entriesToTransfer.stream()
                    .map(entry -> {
                        LocalDateTime ttlDateTime = entry.getTtl().toLocalDateTime();
                        long remainingMinutes = MINUTES.between(LocalDateTime.now(), ttlDateTime);
                        int ttlInMinutes = (int) Math.max(0, remainingMinutes);
                        return new CacheRequest(entry.getKey(), entry.getValue(), ttlInMinutes);
                    })
                    .toList();

            try {
                    restClient.post()
                            .uri(request.getUrl())
                            .body(dataToSend)
                            .retrieve()
                            .toEntity(Void.class);
                    log.debug("Successfully transferred cache entries");
            } catch (Exception e) {
                    log.info("Failed to transfer cache entry, {}", e.getMessage());
            }

            log.info("Data transfer completed for hash range [{}, {}]",
                     request.getStartHash(), request.getEndHash());

        } catch (Exception e) {
            log.error("Error during data transfer", e);
        }
    }
}
