package com.cluster.Node.replication.client;

import com.cluster.Node.replication.model.ReplicationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST implementation of {@link ReplicationClient}. Posts the replication payload to the
 * target replica's internal replication endpoint.
 */
@Slf4j
@Component
public class RestReplicationClient implements ReplicationClient {

    static final String REPLICATE_PATH = "/internal/replicate";

    private final RestClient restClient;

    public RestReplicationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void send(ReplicationTask task) {
        String url = task.getTarget().baseUrl() + REPLICATE_PATH;
        try {
            restClient.post()
                    .uri(url)
                    .body(task.getRequest())
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Replicated {} key={} v{} -> {}",
                    task.getRequest().getOperation(), task.getRequest().getKey(),
                    task.getRequest().getVersion(), url);
        } catch (Exception e) {
            throw new ReplicationDeliveryException("Failed to replicate to " + url, e);
        }
    }
}
