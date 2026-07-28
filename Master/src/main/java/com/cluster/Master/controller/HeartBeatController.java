package com.cluster.Master.controller;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HeartBeatRequest;
import com.cluster.Master.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/heartbeat")
@RequiredArgsConstructor
public class HeartBeatController {

    private final NodeService nodeService;

    @PostMapping
    public ClusterNode heartBeat(@RequestBody HeartBeatRequest request) {
        if(!nodeService.exists(request.getId())){
            return null;
        }
        return nodeService.sendHeartbeat(request);
    }

    @GetMapping
    public List<ClusterNode> findAll() {
        return nodeService.findAll();
    }
}
