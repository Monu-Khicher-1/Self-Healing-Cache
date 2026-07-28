package com.cluster.Master.topology.model;

import com.cluster.Master.topology.state.TransitionState;

/**
 * Snapshot of an in-flight replica-set transition for a partition.
 */
public final class PartitionTransition {

    private final ReplicaSet oldSet;
    private final ReplicaSet newSet;
    private volatile TransitionState state;
    private final long topologyVersion;

    public PartitionTransition(ReplicaSet oldSet, ReplicaSet newSet,
                               TransitionState state, long topologyVersion) {
        this.oldSet = oldSet;
        this.newSet = newSet;
        this.state = state;
        this.topologyVersion = topologyVersion;
    }

    public ReplicaSet oldSet() {
        return oldSet;
    }

    public ReplicaSet newSet() {
        return newSet;
    }

    public TransitionState state() {
        return state;
    }

    public void setState(TransitionState state) {
        this.state = state;
    }

    public long topologyVersion() {
        return topologyVersion;
    }

    public boolean isActive() {
        return state != null && !state.isTerminal();
    }
}
