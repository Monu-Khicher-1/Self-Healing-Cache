package com.cluster.Master.router;


import com.cluster.Master.model.CacheResponse;
import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.DataTransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataTransferRouter {
    private final RestClient restClient;

    public void notifyTransferKeys(ClusterNode sourceNode, ClusterNode targetNode, Long startHash, Long endHash) {
        if (targetNode == null || startHash == null || endHash == null || endHash==-1 || startHash==-1) return;
        if(sourceNode.equals(targetNode)) return;
        try {
            String url = "http://" + sourceNode.getHostname() + ":" + sourceNode.getPort() + "/send-data";

            DataTransferRequest request = DataTransferRequest.builder()
                    .startHash(startHash)
                    .endHash(endHash)
                    .url("http://" + targetNode.getHostname() + ":" + targetNode.getPort() + "/receive-data")
                    .build();
            restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(Void.class);

            log.info("Notified source node {} for data transfer to target node {}", sourceNode.getId(), targetNode.getId());
        } catch (Exception e) {
            log.info("Failed to notify source node {} for data transfer to target node {}: {}", sourceNode.getId(), targetNode.getId(), e.getMessage());
        }
    }
}
