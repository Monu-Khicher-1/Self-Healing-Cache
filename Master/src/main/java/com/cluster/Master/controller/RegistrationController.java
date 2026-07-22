package com.cluster.Master.controller;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.RegistrationRequest;
import com.cluster.Master.service.NodeFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {
    private final NodeFacadeService service;

    @PostMapping
    public ClusterNode set(@RequestBody RegistrationRequest req){
        return service.saveNode(req);
    }

    @GetMapping
    public List<ClusterNode> get(@RequestParam(required = false)  String id){
        if(id==null){
            return service.findAll();
        }
        return List.of(service.getNode(id));
    }

}
