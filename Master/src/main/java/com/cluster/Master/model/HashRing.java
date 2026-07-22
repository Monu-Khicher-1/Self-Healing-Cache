package com.cluster.Master.model;

import com.cluster.Master.utils.HashingStrategy;
import com.cluster.Master.utils.HashingStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.TreeMap;
import jakarta.annotation.PostConstruct;

@Component
public class HashRing {
    TreeMap<Long, ClusterNode> map;
    @Value("${cluster.settings.hashing}")
    private String hashingStrategyName;
    private HashingStrategy hashingStrategy;

    HashRing(){
        map = new TreeMap<>();
    }

    @PostConstruct
    public void init(){
        hashingStrategy = HashingStrategyFactory.getHashingStrategy(hashingStrategyName);
    }

    public void addNode(ClusterNode node) {
        long hash = hashingStrategy.getNodeHash(node.getId());
        map.put(hash, node);
    }
    public void removeNode(String id) {
        map.remove(hashingStrategy.getNodeHash(id));
    }

    public ClusterNode getNode(String key) {
        // code for returning left most neighbour
        long keyHash = hashingStrategy.getKeyHash(key);
        return getNode(keyHash);
    }

    public ClusterNode getNode(Long keyHash) {
        var floorEntry = map.floorEntry(keyHash);
        if(floorEntry != null) return floorEntry.getValue();
        if(map.isEmpty()) return null;
        return map.lastEntry().getValue();
    }

    public ClusterNode getNode(ClusterNode node){
        // get node just before the given node in the ring
        if(map.isEmpty()) return null;
        long keyHash=hashingStrategy.getNodeHash(node.getId());
        return getNode(keyHash-1);
    }

    public long endHash(ClusterNode node){
        if(map.isEmpty() || node==null) return -1;
        long keyHash=hashingStrategy.getNodeHash(node.getId());
        var ceilHash = map.ceilingKey(keyHash+1);
        if(ceilHash == null){
            var firstEntry = map.firstEntry();
            if(firstEntry != null && hashingStrategy.getNodeHash(firstEntry.getValue().getId())!=(keyHash)) {
                return hashingStrategy.getNodeHash(firstEntry.getValue().getId());
            }
        }
        return -1;
    }

    public long startHash(ClusterNode node){
        if(map.isEmpty() || node==null) return -1;
        return hashingStrategy.getNodeHash(node.getId());
    }
}
