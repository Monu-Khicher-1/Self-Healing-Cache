package com.cluster.Master.router;


import com.cluster.Master.model.CacheResponse;
import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.DataTransferRequest;
import com.cluster.Master.topology.model.NodeRef;
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
        notifyTransferKeys(
                new NodeRef(sourceNode.getId(), sourceNode.getHostname(), sourceNode.getPort()),
                new NodeRef(targetNode.getId(), targetNode.getHostname(), targetNode.getPort()),
                startHash, endHash);
    }

    /**
     * Streams keys in {@code [startHash, endHash]} directly from {@code source} to {@code target}
     * (never through the master or an intermediate node), reusing the node {@code /send-data} ->
     * {@code /receive-data} path. Used both for rebalancing and for replica healing, where the
     * source is always a <em>live</em> owner that already holds the data.
     */
    public void notifyTransferKeys(NodeRef source, NodeRef target, long startHash, long endHash) {
        if (source == null || target == null) return;
        if (source.equals(target)) return;
        try {
            String url = source.baseUrl() + "/send-data";

            DataTransferRequest request = DataTransferRequest.builder()
                    .startHash(startHash)
                    .endHash(endHash)
                    .url(target.baseUrl() + "/receive-data")
                    .build();
            restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(Void.class);

            log.info("Notified source node {} for data transfer to target node {}", source.id(), target.id());
        } catch (Exception e) {
            log.info("Failed to notify source node {} for data transfer to target node {}: {}", source.id(), target.id(), e.getMessage());
        }
    }
}
