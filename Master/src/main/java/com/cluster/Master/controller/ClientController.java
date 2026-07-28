package com.cluster.Master.controller;


import com.cluster.Master.model.CacheRequest;
import com.cluster.Master.model.CacheResponse;
import com.cluster.Master.router.ClientRequestRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class ClientController {

    private final ClientRequestRouter clientRequestRouter;

    @GetMapping
    public CacheResponse getCache(@RequestParam String key) {
        return clientRequestRouter.routeGetRequest(key);
    }

    @PostMapping
    public CacheResponse setCache(@RequestBody CacheRequest request) {
        return clientRequestRouter.routePostRequest(request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCache(@RequestParam String key) {
        boolean ok = clientRequestRouter.routeDeleteRequest(key);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.internalServerError().build();
    }
}
