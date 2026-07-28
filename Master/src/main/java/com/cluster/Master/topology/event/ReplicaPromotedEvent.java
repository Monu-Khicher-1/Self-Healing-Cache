package com.cluster.Master.topology.event;

import com.cluster.Master.topology.model.NodeRef;

/**
 * Published after a replica is promoted to primary for a partition. Observers (metrics, clients
 * waiting to be notified, future leader-election auditors) can react without the promotion manager
 * knowing about them.
 */
public record ReplicaPromotedEvent(long anchor, NodeRef deadPrimary, NodeRef newPrimary, long topologyVersion) {
}
