package com.cluster.Node.controller;


import com.cluster.Node.model.cache.CacheEntry;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class CacheController {
    private final CacheService cacheService;

    @GetMapping
    CacheEntry get(@RequestParam(required = false) String key) {
        return cacheService.getEntry(key);
    }

    @PostMapping
    CacheEntry set(@RequestBody CacheRequest request) {
        return cacheService.setEntry(request);
    }

}
