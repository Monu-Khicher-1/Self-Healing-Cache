package com.cluster.Master.service;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.RegistrationRequest;
import com.cluster.Master.topology.PartitionRegistry;
import com.cluster.Master.topology.TopologyManager;
import com.cluster.Master.topology.migration.MigrationCoordinator;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class NodeFacadeService {
    private final NodeService nodeService;
    private final RebalanceService rebalanceService;
    private final TopologyManager topologyManager;
    private final PartitionRegistry partitionRegistry;
    private final MigrationCoordinator migrationCoordinator;

    public ClusterNode getNode(String id) {
            return nodeService.findById(id);
    }

    /**
     * Join flow (req 4): capture the pre-join partition ownership, then record the join — a single
     * atomic step in {@link com.cluster.Master.topology.ClusterTopology} that adds the node to the
     * ring and bumps the version together — and finally hand off to the {@link MigrationCoordinator}
     * to <em>plan</em> and asynchronously execute replica-set transitions. We no longer eagerly move
     * keys here — data is migrated in the background while clients keep reading and writing, using
     * dual-write to the old∪new owners and read-fallback to the old owners until each transition
     * completes.
     */
    public ClusterNode saveNode(RegistrationRequest req){
        List<Partition> prePartitions = partitionRegistry.partitions();
        ClusterNode savedNode = nodeService.save(req);
        topologyManager.recordJoin(savedNode);
        NodeRef joined = new NodeRef(savedNode.getId(), savedNode.getHostname(), savedNode.getPort());
        migrationCoordinator.onNodeJoined(joined, prePartitions);
        return savedNode;
    }

    /**
     * Graceful departure: hand keys off to the successor <em>then</em> leave. Only safe when the
     * node is still alive (it is the data source). Failure eviction must use {@link #evictNode}.
     */
    public void removeNode(ClusterNode node){
        rebalanceService.rebalanceKeysForRemoval(node);
        nodeService.remove(node.getId());
        topologyManager.recordLeave(node);
    }

    /**
     * Failure eviction: no graceful hand-off (the node is dead and cannot stream its data).
     * {@code recordFail} removes it from the ring — immediately making its successor the new primary
     * at the routing layer — and bumps the version. Explicit promotion bookkeeping and replica
     * healing are driven separately by the FailoverManager.
     */
    public void evictNode(ClusterNode node){
        nodeService.remove(node.getId());
        topologyManager.recordFail(node);
    }

    public List<ClusterNode> findAll(){
        return nodeService.findAll();
    }

}
