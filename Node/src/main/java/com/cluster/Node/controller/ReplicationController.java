package com.cluster.Node.controller;

import com.cluster.Node.replication.ReplicationApplyService;
import com.cluster.Node.replication.model.ReplicationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal node-to-node replication API. A primary's replication workers call this endpoint
 * on each replica. This path deliberately does NOT re-replicate, preventing fan-out loops.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/replicate")
public class ReplicationController {

    private final ReplicationApplyService replicationApplyService;

    @PostMapping
    public void replicate(@RequestBody ReplicationRequest request) {
        log.debug("Received replication {} for key {} v{}",
                request.getOperation(), request.getKey(), request.getVersion());
        replicationApplyService.apply(request);
    }
}
