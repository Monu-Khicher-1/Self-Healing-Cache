package com.cluster.Node.controller;


import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.replication.ReplicationManager;
import com.cluster.Node.replication.model.ReplicaEndpoint;
import com.cluster.Node.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Client-facing cache API on the primary node. Writes are stored locally and then handed to
 * the {@link ReplicationManager} for asynchronous fan-out to the replica endpoints the master
 * attached to the request.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class CacheController {
    private final CacheService cacheService;
    private final ReplicationManager replicationManager;

    @GetMapping
    CacheEntry get(@RequestParam(required = false) String key) {
        return cacheService.getEntry(key);
    }

    @PostMapping
    CacheEntry set(@RequestBody CacheRequest request) {
        CacheEntry entry = cacheService.setEntry(request);
        replicationManager.replicatePut(entry, request.getReplicas());
        return entry;
    }

    @DeleteMapping
    void delete(@RequestParam String key,
                @RequestBody(required = false) List<ReplicaEndpoint> replicas) {
        long version = cacheService.deleteEntry(key);
        replicationManager.replicateDelete(key, version, replicas);
    }

}
