package com.cluster.Master.topology.state;

import java.util.Set;

/**
 * Role of a node within a partition's replica set.
 *
 * <pre>
 * REPLICA       -> PROMOTING | SYNCHRONIZING
 * PROMOTING     -> PRIMARY | REPLICA        (promotion succeeded, or aborted)
 * PRIMARY       -> REPLICA                  (demoted, e.g. old primary rejoins)
 * SYNCHRONIZING -> REPLICA                  (finished catching up)
 * </pre>
 *
 * Promotion is deliberately modelled as a role transition only (no key movement), matching how
 * Redis Cluster and Hazelcast promote an existing backup that already holds the data.
 */
public enum ReplicaRole implements State<ReplicaRole> {
    PRIMARY,
    REPLICA,
    PROMOTING,
    SYNCHRONIZING;

    @Override
    public Set<ReplicaRole> allowedTransitions() {
        return switch (this) {
            case PRIMARY -> Set.of(REPLICA);
            case REPLICA -> Set.of(PROMOTING, SYNCHRONIZING);
            case PROMOTING -> Set.of(PRIMARY, REPLICA);
            case SYNCHRONIZING -> Set.of(REPLICA);
        };
    }
}
