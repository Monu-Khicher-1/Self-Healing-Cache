package com.cluster.Node.controller;


import com.cluster.Node.model.DataTransferRequest;
import com.cluster.Node.model.cache.CacheRequest;
import com.cluster.Node.service.CacheService;
import com.cluster.Node.service.DataTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Slf4j
public class DataTransferController {

    private final CacheService cacheService;
    private final DataTransferService dataTransferService;

    @PostMapping("/send-data")
    void sendData(@RequestBody DataTransferRequest request) {
        // Implement to start data transfer to the node specified in the request
        log.info("Starting data transfer to node: {}", request.getUrl());
        dataTransferService.transferData(request);
    }

    @PostMapping("/receive-data")
    void receiveData(@RequestBody List<CacheRequest> data) {
        // receiving transfered data from old node
        for (CacheRequest request : data) {
            cacheService.setEntry(request);
        }
    }

}
