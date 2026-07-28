package com.cluster.Node.controller;

import com.cluster.Node.model.RangeRequest;
import com.cluster.Node.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Master-facing maintenance endpoints used by the migration completion barrier and cleanup:
 *
 * <ul>
 *   <li>{@code GET /internal/range-count} — how many entries this node holds in a hash range, so
 *       the master can compare a source's count against each new owner before completing a
 *       transition (req 23 verification).</li>
 *   <li>{@code POST /internal/drop-range} — remove entries in a hash range, so obsolete copies are
 *       purged from an old owner once a transition completes (req 5/24 cleanup).</li>
 * </ul>
 *
 * These are intentionally separate from the client cache API — they operate on raw hash ranges and
 * are only ever called node-to-master, never by clients.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalMaintenanceController {

    private final CacheRepository cacheRepository;

    @GetMapping("/range-count")
    public long rangeCount(@RequestParam long startHash, @RequestParam long endHash) {
        return cacheRepository.countByHashRange(startHash, endHash);
    }

    @PostMapping("/drop-range")
    public long dropRange(@RequestBody RangeRequest request) {
        log.info("Dropping entries in hash range ({}, {}]", request.getStartHash(), request.getEndHash());
        return cacheRepository.removeByHashRange(request.getStartHash(), request.getEndHash());
    }
}
