package com.cluster.Master.repository;


import com.cluster.Master.model.ClusterNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class NodeRepository {
    private int key;
    private ConcurrentHashMap<String, ClusterNode> clusterNodeMap;
    public NodeRepository() {
        clusterNodeMap = new ConcurrentHashMap<>();
        key = 0;
    }

    public boolean exists(String id) {
        return clusterNodeMap.containsKey(id);
    }

    public ClusterNode findById(String id){
        return clusterNodeMap.get(id);
    }


    public ClusterNode save(ClusterNode clusterNode){
        clusterNode.setId(String.valueOf(++key));
        clusterNodeMap.put(clusterNode.getId(),clusterNode);
        return clusterNode;
    }

    public List<ClusterNode> findAll(){
        return new ArrayList<>(clusterNodeMap.values());
    }

    public ClusterNode updateHeartBeat(String id){
        ClusterNode clusterNode = findById(id);
        if(clusterNode == null){
            log.info("No object found for id:{}",id);
            return null;
        }
        clusterNode.setLastHeartbeat(Timestamp.valueOf(LocalDateTime.now()));
        clusterNodeMap.put(clusterNode.getId(),clusterNode);
        log.info("Cluster node updated heartbeat:{}",clusterNode);
        return clusterNode;
    }

    public void remove(String id){
        clusterNodeMap.remove(id);
    }




}
