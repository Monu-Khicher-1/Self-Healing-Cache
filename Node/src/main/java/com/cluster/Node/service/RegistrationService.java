package com.cluster.Node.service;


import com.cluster.Node.model.register.RegistrationRequest;
import com.cluster.Node.model.register.RegistrationResponse;
import com.cluster.Node.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final NodeRepository nodeRepository;
    private final RestClient restClient;
    private final Environment environment;

    @Value("${cluster.registry.url}")
    private String url;

    @Value("${cluster.node.host}")
    private String nodeHost;


    @EventListener(ApplicationReadyEvent.class)
    public void register(){
        int port = environment.getProperty("server.port", Integer.class, 8082);
        RegistrationRequest registrationRequest = new RegistrationRequest(nodeHost, port);

        try {
            RegistrationResponse response = restClient.post()
                    .uri(url)
                    .body(registrationRequest)
                    .retrieve()
                    .body(RegistrationResponse.class);

            log.info("Response body is {}", response);
            if (response != null) {
                nodeRepository.setNode(response);
            }
        } catch (RestClientException ex) {
            log.error("Registration failed for {}:{} against {}: {}", nodeHost, port, url, ex.getMessage());
        }
    }
}
