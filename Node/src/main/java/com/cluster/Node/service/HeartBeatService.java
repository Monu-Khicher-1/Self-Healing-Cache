package com.cluster.Node.service;


import com.cluster.Node.model.register.RegistrationResponse;
import com.cluster.Node.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartBeatService {

    private final NodeRepository nodeRepository;
    private final RestClient restClient;

    @Value("${cluster.heartbeat.url}")
    private String url;

    @Scheduled(fixedRate = 60000)
    public void sendheartBeat() {

        String id = nodeRepository.getNodeId();
        if(id==null){
            log.info("Not able to send heartbeat with id is null");
            return;
        }
        RegistrationResponse update = restClient.post()
                .uri(url)
                .body(id)
                .retrieve()
                        .body(RegistrationResponse.class);

        if(update==null) return;

        log.info("Updated Heartbeat: {}", update.getLastHeartbeat());
        log.info("Sending heartbeat request to node {}", id);

    }
}
