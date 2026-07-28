package com.cluster.Node.topology;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives authoritative topology snapshots pushed by the master after every membership change.
 * Kept deliberately thin: it just hands the snapshot to {@link LocalTopologyView}, which applies
 * version-gating. This endpoint is the node-side seam a gossip protocol could later replace.
 */
@RestController
@RequestMapping("/internal/topology")
@RequiredArgsConstructor
public class TopologyController {

    private final LocalTopologyView localTopologyView;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody TopologySnapshot snapshot) {
        localTopologyView.apply(snapshot);
        return ResponseEntity.accepted().build();
    }
}
