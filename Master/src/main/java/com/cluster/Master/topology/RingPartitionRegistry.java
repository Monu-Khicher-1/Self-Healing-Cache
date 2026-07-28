package com.cluster.Master.topology;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.ReplicaSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PartitionRegistry} backed by the consistent-hash ring. Replica placement reuses
 * {@link HashRing#getNodesFromHash(long, int)} so it is guaranteed identical to the routing
 * path, preserving behaviour parity while introducing the partition abstraction.
 */
@Component
@RequiredArgsConstructor
public class RingPartitionRegistry implements PartitionRegistry {

    private final ClusterTopology topology;

    @Override
    public ReplicaSet replicaSetForKey(String key) {
        int factor = topology.factor();
        HashRing hashRing = topology.ring();
        List<ClusterNode> owners = hashRing.getNodes(key, factor);
        return ReplicaSet.of(toRefs(owners), topology.version());
    }

    @Override
    public List<Partition> partitions() {
        int factor = topology.factor();
        long version = topology.version();
        HashRing hashRing = topology.ring();
        List<Partition> partitions = new ArrayList<>();
        for (long anchor : hashRing.ringHashes()) {
            ClusterNode owner = hashRing.nodeAtHash(anchor);
            if (owner == null) {
                continue;
            }
            long successor = hashRing.successorHash(anchor);
            long rangeEnd = (successor == anchor) ? anchor - 1 : successor - 1;
            ReplicaSet replicaSet = ReplicaSet.of(
                    toRefs(hashRing.getNodesFromHash(anchor, factor)), version);
            partitions.add(new Partition(owner.getId(), anchor, rangeEnd, replicaSet));
        }
        return partitions;
    }

    static NodeRef toRef(ClusterNode node) {
        return new NodeRef(node.getId(), node.getHostname(), node.getPort());
    }

    private static List<NodeRef> toRefs(List<ClusterNode> nodes) {
        List<NodeRef> refs = new ArrayList<>(nodes.size());
        for (ClusterNode node : nodes) {
            refs.add(toRef(node));
        }
        return refs;
    }
}
