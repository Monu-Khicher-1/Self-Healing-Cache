package com.cluster.Master.service;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.topology.failover.FailoverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Detects dead nodes via heartbeat expiry and hands each failure to the {@link FailoverManager},
 * which evicts the node, promotes healthy replicas, and restores the replication factor. This
 * class owns <em>detection</em> only; recovery policy lives behind the FailoverManager interface,
 * so swapping heartbeat for gossip later needs no change here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {
    private final NodeService nodeService;
    private final FailoverManager failoverManager;

    @Scheduled(fixedRateString = "${cluster.failure.check-interval-ms:60000}")
    public void removeInActiveClusterNodes() {
        List<String> expiredNodes = nodeService.getExpired();
        for (String id : expiredNodes) {
            ClusterNode node = nodeService.findById(id);
            if (node == null) {
                continue;
            }
            log.warn("Node {} failed heartbeat check; initiating failover", id);
            failoverManager.onNodeFailed(node);
        }
    }
}
