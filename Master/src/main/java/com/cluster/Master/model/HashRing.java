package com.cluster.Master.model;

import com.cluster.Master.utils.HashingStrategy;
import com.cluster.Master.utils.HashingStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import jakarta.annotation.PostConstruct;

@Component
public class HashRing {
    ConcurrentSkipListMap<Long, ClusterNode> map;
    @Value("${cluster.settings.hashing}")
    private String hashingStrategyName;
    private HashingStrategy hashingStrategy;

    HashRing(){
        map = new ConcurrentSkipListMap<>();
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

    /** Exposes the ring's key-hash function so ownership/transition lookups agree with routing. */
    public long keyHash(String key) {
        return hashingStrategy.getKeyHash(key);
    }

    public ClusterNode getNode(Long keyHash) {
        var floorEntry = map.floorEntry(keyHash);
        if(floorEntry != null) return floorEntry.getValue();
        if(map.isEmpty()) return null;
        return map.lastEntry().getValue();
    }

    /**
     * Returns up to {@code count} distinct physical nodes responsible for the key, walking the
     * ring clockwise starting from the primary. The first element is the primary; the rest are
     * replicas. Fewer than {@code count} nodes are returned when the cluster is smaller.
     */
    public List<ClusterNode> getNodes(String key, int count) {
        if (map.isEmpty() || count <= 0) return new ArrayList<>();
        return getNodesFromHash(hashingStrategy.getKeyHash(key), count);
    }

    /**
     * Clockwise distinct-node walk starting from {@code floor(anchorHash)}. Shared by key-based
     * lookup and partition/replica-set construction so both use identical placement logic.
     */
    public List<ClusterNode> getNodesFromHash(long anchorHash, int count) {
        List<ClusterNode> result = new ArrayList<>();
        if (map.isEmpty() || count <= 0) return result;

        Long cursor = map.floorKey(anchorHash);
        if (cursor == null) cursor = map.lastKey();

        Set<String> seen = new HashSet<>();
        int ringSize = map.size();
        int visited = 0;
        while (result.size() < count && visited < ringSize) {
            ClusterNode node = map.get(cursor);
            if (node != null && seen.add(node.getId())) {
                result.add(node);
            }
            Long next = map.higherKey(cursor);
            cursor = (next == null) ? map.firstKey() : next;
            visited++;
        }
        return result;
    }

    /** Ordered ring positions (node hashes), ascending. */
    public List<Long> ringHashes() {
        return new ArrayList<>(map.keySet());
    }

    /** All distinct physical nodes currently on the ring (defensive copy). */
    public List<ClusterNode> getAllNodes() {
        return new ArrayList<>(new HashSet<>(map.values()));
    }

    public ClusterNode nodeAtHash(long hash) {
        return map.get(hash);
    }

    /** Next ring position clockwise, wrapping to the first when {@code hash} is the largest. */
    public Long successorHash(long hash) {
        Long next = map.higherKey(hash);
        return (next == null) ? map.firstKey() : next;
    }

    public int size() {
        return map.size();
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
