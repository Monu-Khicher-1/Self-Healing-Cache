package com.cluster.Master.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscoveryService {
    private final NodeService nodeService;

    @Scheduled(fixedRate = 60000)
    public void removeInActiveClusterNodes() {
        List<String> expiredNodes = nodeService.getExpired();
        for (String node : expiredNodes) {
            nodeService.remove(node);
        }
    }
}
