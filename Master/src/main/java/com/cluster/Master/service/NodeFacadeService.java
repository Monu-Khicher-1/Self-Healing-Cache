package com.cluster.Master.service;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.model.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class NodeFacadeService {
    private final HashRing hashRing;
    private final NodeService nodeService;
    private final RebalanceService rebalanceService;

    public ClusterNode getNode(String id) {
            return nodeService.findById(id);
    }

    public ClusterNode saveNode(RegistrationRequest req){
        ClusterNode savedNode = nodeService.save(req);
        hashRing.addNode(savedNode);
        rebalanceService.rebalanceKeysForAddition(savedNode);
        return savedNode;
    }

    public void removeNode(ClusterNode node){
        nodeService.remove(node.getId());
        hashRing.removeNode(node.getId());
        rebalanceService.rebalanceKeysForRemoval(node);
    }

    public List<ClusterNode> findAll(){
        return nodeService.findAll();
    }

}
