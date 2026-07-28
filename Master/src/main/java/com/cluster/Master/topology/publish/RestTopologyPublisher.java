package com.cluster.Master.topology.publish;

import com.cluster.Master.topology.TopologyManager;
import com.cluster.Master.topology.dto.NodeRefDto;
import com.cluster.Master.topology.dto.TopologySnapshot;
import com.cluster.Master.topology.event.TopologyChangedEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * REST {@link TopologyPublisher}. Listens for {@link TopologyChangedEvent} and pushes the fresh
 * snapshot to every node's {@code /internal/topology} endpoint on a background executor, so a
 * membership change never blocks on node round-trips. Delivery is best-effort per node: nodes
 * also carry the version, so a missed push is corrected by the next one (eventually consistent
 * distribution, as in Hazelcast's partition-table gossip).
 */
@Slf4j
@Component
public class RestTopologyPublisher implements TopologyPublisher {

    static final String TOPOLOGY_PATH = "/internal/topology";

    private final RestClient restClient;
    private final TopologyManager topologyManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "topology-publisher");
        t.setDaemon(true);
        return t;
    });

    public RestTopologyPublisher(RestClient restClient, TopologyManager topologyManager) {
        this.restClient = restClient;
        this.topologyManager = topologyManager;
    }

    @EventListener
    public void onTopologyChanged(TopologyChangedEvent event) {
        TopologySnapshot snapshot = topologyManager.currentSnapshot();
        executor.submit(() -> publish(snapshot));
    }

    @Override
    public void publish(TopologySnapshot snapshot) {
        for (NodeRefDto node : snapshot.nodes()) {
            String url = "http://" + node.host() + ":" + node.port() + TOPOLOGY_PATH;
            try {
                restClient.post()
                        .uri(url)
                        .body(snapshot)
                        .retrieve()
                        .toBodilessEntity();
                log.debug("Published topology v{} to node {}", snapshot.version(), node.id());
            } catch (Exception e) {
                log.warn("Failed to publish topology v{} to node {} ({}): {}",
                        snapshot.version(), node.id(), url, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
