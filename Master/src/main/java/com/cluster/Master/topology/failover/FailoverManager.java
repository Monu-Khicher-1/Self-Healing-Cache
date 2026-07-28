package com.cluster.Master.topology.failover;

import com.cluster.Master.model.ClusterNode;

/**
 * Coordinates recovery when a node is detected dead. It sequences the two independent concerns —
 * replica <em>promotion</em> (metadata only) and replica <em>healing</em> (data movement) — but
 * owns neither, delegating to {@link ReplicaPromotionManager} and {@link ReplicaHealingService}.
 * Failure detection ({@code DiscoveryService}) depends only on this interface, so the detection
 * mechanism (heartbeat now, gossip later) can change without touching recovery.
 */
public interface FailoverManager {

    /**
     * Handles the failure of {@code node}: captures the pre-failure replica sets, evicts the node
     * from the ring/registry/topology, promotes healthy replicas for partitions it led, and
     * restores the replication factor for every partition it belonged to.
     */
    void onNodeFailed(ClusterNode node);
}
