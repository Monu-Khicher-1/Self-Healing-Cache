package com.cluster.Master.controller;

import com.cluster.Master.replication.FailureRecoveryManager;
import com.cluster.Master.topology.TopologyManager;
import com.cluster.Master.topology.dto.TopologySnapshot;
import com.cluster.Master.topology.model.NodeRef;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Read-only, client-facing view of cluster topology (req 8). Today clients talk only to the master;
 * these endpoints let a future smart client cache the topology and route directly to owning nodes,
 * comparing its cached {@code version} against {@link #version()} to detect staleness — exactly how
 * Redis Cluster clients cache the slot map (bumping on {@code MOVED}) and Hazelcast clients track
 * the partition-table version.
 *
 * <ul>
 *   <li>{@code GET /topology} — the full versioned snapshot (nodes + partitions + replica sets).</li>
 *   <li>{@code GET /topology/version} — just the current version, a cheap staleness poll.</li>
 *   <li>{@code GET /topology/route?key=} — ordered owners for a key (read-preference order),
 *       so a client can resolve a key to nodes without embedding the ring/hash logic yet.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/topology")
public class TopologyController {

    private final TopologyManager topologyManager;
    private final FailureRecoveryManager failureRecoveryManager;

    @GetMapping
    public TopologySnapshot snapshot() {
        return topologyManager.currentSnapshot();
    }

    @GetMapping("/version")
    public Map<String, Long> version() {
        return Map.of("version", topologyManager.currentVersion());
    }

    @GetMapping("/route")
    public Map<String, Object> route(@RequestParam String key) {
        List<NodeRef> owners = failureRecoveryManager.getReadCandidates(key);
        return Map.of(
                "key", key,
                "version", topologyManager.currentVersion(),
                "owners", owners);
    }
}
