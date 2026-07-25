package com.cluster.Node.service;


import com.cluster.Node.model.register.HeartBeatRequest;
import com.cluster.Node.model.register.RegistrationResponse;
import com.cluster.Node.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartBeatService {

    private final NodeRepository nodeRepository;
    private final RestClient restClient;

    @Value("${cluster.heartbeat.url}")
    private String url;

    @Value("${cluster.node.host}")
    private String nodeHost;

    @Value("${server.port}")
    private int nodePort;

    @Scheduled(fixedRate = 60000)
    public void sendheartBeat() {

        String id = nodeRepository.getNodeId();
        if(id==null){
            log.info("Not able to send heartbeat with id is null");
            return;
        }

        HeartBeatRequest request = new HeartBeatRequest(id, nodeHost, nodePort);

        try {
            RegistrationResponse update = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(RegistrationResponse.class);

            if (update == null) {
                return;
            }

            log.info("Updated Heartbeat: {}", update.getLastHeartbeat());
            log.info("Sending heartbeat request to node {}", id);
        } catch (RestClientException ex) {
            log.error("Failed heartbeat for node {} to {}: {}", id, url, ex.getMessage());
        }

    }
}
