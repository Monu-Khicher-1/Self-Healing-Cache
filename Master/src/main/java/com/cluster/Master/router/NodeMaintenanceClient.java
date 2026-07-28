package com.cluster.Master.router;

import com.cluster.Master.topology.model.NodeRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Thin client for the node {@code /internal/*} maintenance endpoints used by the migration
 * completion barrier. Kept separate from {@link DataTransferRouter} (which moves data) so that
 * verification/cleanup concerns stay isolated from bulk copy (single responsibility).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeMaintenanceClient {

    private final RestClient restClient;

    /**
     * Count of entries a node holds in {@code (startHash, endHash]}, or {@code -1} if the node
     * could not be reached (so callers can distinguish "empty" from "unknown").
     */
    public long countRange(NodeRef node, long startHash, long endHash) {
        try {
            Long count = restClient.get()
                    .uri(node.baseUrl() + "/internal/range-count?startHash={s}&endHash={e}",
                            startHash, endHash)
                    .retrieve()
                    .body(Long.class);
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("range-count failed on node {} for ({},{}]: {}", node.id(), startHash, endHash, e.getMessage());
            return -1;
        }
    }

    /** Drops entries in {@code (startHash, endHash]} on a node; returns removed count or -1 on error. */
    public long dropRange(NodeRef node, long startHash, long endHash) {
        try {
            Long removed = restClient.post()
                    .uri(node.baseUrl() + "/internal/drop-range")
                    .body(Map.of("startHash", startHash, "endHash", endHash))
                    .retrieve()
                    .body(Long.class);
            log.info("Dropped {} obsolete entries on node {} for range ({},{}]",
                    removed, node.id(), startHash, endHash);
            return removed == null ? 0 : removed;
        } catch (Exception e) {
            log.warn("drop-range failed on node {} for ({},{}]: {}", node.id(), startHash, endHash, e.getMessage());
            return -1;
        }
    }
}
