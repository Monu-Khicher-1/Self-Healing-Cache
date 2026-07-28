package com.cluster.Master.topology;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.topology.dto.TopologySnapshot;
import com.cluster.Master.topology.state.NodeState;

/**
 * Authoritative owner of cluster topology on the master. It is the only component permitted to
 * mutate topology state; everything else observes it (via {@link ReplicaMetadataService} /
 * {@link #currentSnapshot()}) or reacts to the {@code TopologyChangedEvent} it emits. This
 * single-writer design is what lets a gossip/consensus layer replace it later without changing
 * replication or routing.
 */
public interface TopologyManager {

    long currentVersion();

    /** Node lifecycle state, or {@code null} if unknown. */
    NodeState nodeState(String nodeId);

    /** Records a newly registered node, bumps the version, and emits a change event. */
    void recordJoin(ClusterNode node);

    /** Records a graceful node departure, bumps the version, and emits a change event. */
    void recordLeave(ClusterNode node);

    /** Records a node failure eviction (node marked DEAD), bumps the version, emits an event. */
    void recordFail(ClusterNode node);

    /** Records a previously-dead node recovering, bumps the version, and emits a change event. */
    void recordRecover(ClusterNode node);

    /** Builds a fresh snapshot of the current topology for publishing to nodes/clients. */
    TopologySnapshot currentSnapshot();
}
