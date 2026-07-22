package com.cluster.Master.controller;


import com.cluster.Master.model.ClusterNode;
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
    public ClusterNode heartBeat(@RequestBody String id) {
        if(!nodeService.exists(id)){
            return null;
        }
        return nodeService.sendHeartbeat(id);
    }

    @GetMapping
    public List<ClusterNode> findAll() {
        return nodeService.findAll();
    }
}
