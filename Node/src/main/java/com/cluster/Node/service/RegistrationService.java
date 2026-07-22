package com.cluster.Node.service;


import com.cluster.Node.model.register.RegistrationRequest;
import com.cluster.Node.model.register.RegistrationResponse;
import com.cluster.Node.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final NodeRepository nodeRepository;
    private final RestClient restClient;
    private final Environment environment;
    @Value("${cluster.registry.url}")
    private String url;


    @EventListener(ApplicationReadyEvent.class)
    public void register(){
        int port = environment.getProperty("server.port", Integer.class);
        RegistrationRequest registrationRequest = new RegistrationRequest("localhost",port);


        RegistrationResponse response = restClient.post()
                .uri(url)
                .body(registrationRequest)
                .retrieve()
                .body(RegistrationResponse.class);

        log.info("Response body is {}", response);

        RegistrationResponse registrationResponse = response;
        if(registrationResponse!=null) nodeRepository.setNode(registrationResponse);
    }
}
