package com.cluster.Master.router;

import com.cluster.Master.model.CacheRequest;
import com.cluster.Master.model.CacheResponse;
import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.service.KeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class ClientRequestRouter {
    private final KeyService keyService;
    private final RestClient restClient;


    public CacheResponse routeGetRequest(String key) {
        try {
            ClusterNode node = keyService.getNodeForKey(key);
            if (node == null) {
                log.error("No node found for key: {}", key);
                return null;
            }
            
            String url = "http://" + node.getHostname() + ":" + node.getPort() + "/cache?key=" + key;
            log.debug("Target Node - Hostname: {}, Port: {}", node.getHostname(), node.getPort());
            log.info("Routing request to URL: {}", url);
            
            CacheResponse cacheResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(CacheResponse.class);

            if (cacheResponse == null) {
                log.warn("Received null response from node: {}", url);
            } else {
                log.debug("Response received - Data: {}", cacheResponse);
                log.info("Successfully retrieved cache response for key: {}", key);
            }

            return cacheResponse;
        } catch (Exception e) {
            log.error("Error routing request for key: {}. Exception: {}", key, e.getMessage(), e);
            return null;
        }
    }

    public CacheResponse routePostRequest(CacheRequest request){
        try {
            ClusterNode node = keyService.getNodeForKey(request.getKey());
            if (node == null) {
                log.error("No node found for key: {}", request.getKey());
            }
            String url = "http://" + node.getHostname() + ":" + node.getPort() + "/cache";
            log.debug("Target Node - Hostname: {}, Port: {}", node.getHostname(), node.getPort());
            log.info("Routing request to URL: {}", url);

            CacheResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(CacheResponse.class);
            if (response == null) {
                log.error("Received null response from node: {}", url);
            }
            return response;
        } catch (Exception e) {
            log.error("Error routing request for key: {}. Exception: {}", request.getKey(), e.getMessage(), e);
            return null;
        }

    }
}
