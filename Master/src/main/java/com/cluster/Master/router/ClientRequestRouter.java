package com.cluster.Master.router;

import com.cluster.Master.model.CacheRequest;
import com.cluster.Master.model.CacheResponse;
import com.cluster.Master.model.ReplicaEndpoint;
import com.cluster.Master.replication.FailureRecoveryManager;
import com.cluster.Master.topology.model.NodeRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;


/**
 * Routes client cache operations to nodes. Writes go to the primary only, carrying the replica
 * endpoints so the primary can fan out asynchronously. Reads try the primary first and fail
 * over to replicas in order, giving availability when the primary is down but not yet evicted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientRequestRouter {
    private final FailureRecoveryManager failureRecoveryManager;
    private final RestClient restClient;

    public CacheResponse routeGetRequest(String key) {
        List<NodeRef> candidates = failureRecoveryManager.getReadCandidates(key);
        if (candidates.isEmpty()) {
            log.error("No node found for key: {}", key);
            return null;
        }
        log.info("Get node candidate for key: {}  are {}", key, candidates);
        for (NodeRef node : candidates) {
            String url = baseUrl(node) + "/cache?key=" + key;
            try {
                log.info("Routing GET for key {} to {}", key, url);
                CacheResponse response = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(CacheResponse.class);
                if (response != null) {
                    return response;
                }
                log.warn("Node {} returned no data for key {}; trying next replica", node.id(), key);
            } catch (Exception e) {
                log.warn("GET failed on node {} for key {} ({}); failing over", node.id(), key, e.getMessage());
            }
        }
        log.error("All candidates failed for key: {}", key);
        return null;
    }

    public CacheResponse routePostRequest(CacheRequest request) {
        List<NodeRef> targets = failureRecoveryManager.getWriteTargets(request.getKey());
        if (targets.isEmpty()) {
            log.error("No node found for key: {}", request.getKey());
            return null;
        }
        NodeRef primary = targets.get(0);
        request.setReplicas(toReplicaEndpoints(targets));

        String url = baseUrl(primary) + "/cache";
        try {
            log.info("Routing PUT for key {} to primary {} with {} replica(s)",
                    request.getKey(), primary.id(), request.getReplicas().size());
            return restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(CacheResponse.class);
        } catch (Exception e) {
            log.error("Error routing PUT for key {}: {}", request.getKey(), e.getMessage(), e);
            return null;
        }
    }

    public boolean routeDeleteRequest(String key) {
        List<NodeRef> targets = failureRecoveryManager.getWriteTargets(key);
        if (targets.isEmpty()) {
            log.error("No node found for key: {}", key);
            return false;
        }
        NodeRef primary = targets.get(0);
        List<ReplicaEndpoint> replicas = toReplicaEndpoints(targets);

        String url = baseUrl(primary) + "/cache?key=" + key;
        try {
            log.info("Routing DELETE for key {} to primary {} with {} replica(s)",
                    key, primary.id(), replicas.size());
            restClient.method(HttpMethod.DELETE)
                    .uri(url)
                    .body(replicas)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Error routing DELETE for key {}: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Maps ordered targets to replica endpoints. Index 0 is the primary and is excluded, so the
     * primary never replicates to itself.
     */
    private List<ReplicaEndpoint> toReplicaEndpoints(List<NodeRef> targets) {
        return targets.stream()
                .skip(1)
                .map(ReplicaEndpoint::from)
                .toList();
    }

    private String baseUrl(NodeRef node) {
        return "http://" + node.host() + ":" + node.port();
    }
}
