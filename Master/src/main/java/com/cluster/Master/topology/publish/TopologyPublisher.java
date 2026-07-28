package com.cluster.Master.topology.publish;

import com.cluster.Master.topology.dto.TopologySnapshot;

/**
 * Distributes an authoritative {@link TopologySnapshot} to cache nodes after a topology change.
 * Abstracted so the transport (REST now, gossip later) can change without touching the manager.
 */
public interface TopologyPublisher {

    void publish(TopologySnapshot snapshot);
}
