package com.cluster.Master.service;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeyService {
    private final HashRing hashRing;
    public void addKey(String key){
        //TODO
    }
    public void removeKey(String key){
        //TODO
    }
    public ClusterNode getNodeForKey(String key) {
        return hashRing.getNode(key);
    }

    public void transferKeys(ClusterNode sourceNode, ClusterNode targetNode) {
        // Implement the logic to transfer keys from sourceNode to targetNode
        // This is a placeholder for the actual implementation
    }
}
