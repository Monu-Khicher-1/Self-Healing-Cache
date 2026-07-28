package com.cluster.Master.topology;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.topology.dto.NodeRefDto;
import com.cluster.Master.topology.dto.PartitionView;
import com.cluster.Master.topology.dto.ReplicaSetView;
import com.cluster.Master.topology.dto.TopologySnapshot;
import com.cluster.Master.topology.event.TopologyChangedEvent;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.PartitionTransition;
import com.cluster.Master.topology.model.ReplicaSet;
import com.cluster.Master.topology.state.NodeState;
import com.cluster.Master.topology.state.StateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutation policy and event publishing layer. Delegates all state to ClusterTopology,
 * publishes TopologyChangedEvent after every change for subscribers to respond.
 * Separation of policy from state enables future gossip/consensus swaps.
 */
@Slf4j
@Service
public class DefaultTopologyManager implements TopologyManager {

    private final ClusterTopology topology;
    private final PartitionRegistry partitionRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultTopologyManager(ClusterTopology topology,
                                  PartitionRegistry partitionRegistry,
                                  ApplicationEventPublisher eventPublisher) {
        this.topology = topology;
        this.partitionRegistry = partitionRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public long currentVersion() {
        return topology.version();
    }

    @Override
    public NodeState nodeState(String nodeId) {
        return topology.nodeState(nodeId);
    }

    @Override
    public void recordJoin(ClusterNode node) {
        long version = topology.join(node);
        eventPublisher.publishEvent(new TopologyChangedEvent(version, "join:" + node.getId()));
    }

    @Override
    public void recordLeave(ClusterNode node) {
        long version = topology.leave(node);
        eventPublisher.publishEvent(new TopologyChangedEvent(version, "leave:" + node.getId()));
    }

    @Override
    public void recordFail(ClusterNode node) {
        long version = topology.fail(node);
        eventPublisher.publishEvent(new TopologyChangedEvent(version, "fail:" + node.getId()));
    }

    @Override
    public void recordRecover(ClusterNode node) {
        long version = topology.recover(node);
        eventPublisher.publishEvent(new TopologyChangedEvent(version, "recover:" + node.getId()));
    }

    @Override
    public TopologySnapshot currentSnapshot() {
        List<Partition> partitions = partitionRegistry.partitions();
        Map<String, NodeRefDto> nodes = new LinkedHashMap<>();
        List<PartitionView> partitionViews = new ArrayList<>(partitions.size());

        for (Partition partition : partitions) {
            ReplicaSetView current = toView(partition.current(), nodes);
            ReplicaSetView incoming = null;
            String transitionState = null;
            PartitionTransition transition = partition.transition();
            if (transition != null && transition.isActive()) {
                incoming = toView(transition.newSet(), nodes);
                transitionState = transition.state().name();
            }
            partitionViews.add(new PartitionView(
                    partition.id(), partition.rangeStart(), partition.rangeEnd(),
                    current, incoming, transitionState));
        }
        return new TopologySnapshot(topology.version(),
                new ArrayList<>(nodes.values()), partitionViews);
    }

    private ReplicaSetView toView(ReplicaSet set, Map<String, NodeRefDto> nodeAccumulator) {
        if (set == null) {
            return null;
        }
        NodeRefDto primary = toDto(set.primary(), nodeAccumulator);
        List<NodeRefDto> replicas = new ArrayList<>();
        for (NodeRef replica : set.replicas()) {
            replicas.add(toDto(replica, nodeAccumulator));
        }
        return new ReplicaSetView(primary, replicas, set.version());
    }

    private NodeRefDto toDto(NodeRef ref, Map<String, NodeRefDto> nodeAccumulator) {
        if (ref == null) {
            return null;
        }
        NodeRefDto dto = new NodeRefDto(ref.id(), ref.host(), ref.port());
        nodeAccumulator.putIfAbsent(ref.id(), dto);
        return dto;
    }
}
