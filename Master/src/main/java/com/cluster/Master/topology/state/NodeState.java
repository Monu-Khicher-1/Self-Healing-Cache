package com.cluster.Master.topology.state;

import java.util.Set;

/**
 * Lifecycle of a physical node as tracked by the master.
 *
 * <pre>
 * REGISTERING -> ACTIVE
 * ACTIVE      -> SUSPECT            (heartbeat missed, not yet confirmed dead)
 * SUSPECT     -> ACTIVE | DEAD      (recovered, or grace elapsed)
 * DEAD        -> RECOVERING         (a previously dead node comes back)
 * RECOVERING  -> ACTIVE
 * </pre>
 *
 * The SUSPECT stage mirrors Cassandra's Phi-accrual detector and Redis Cluster's PFAIL flag:
 * a transient miss should not immediately trigger expensive failover.
 */
public enum NodeState implements State<NodeState> {
    REGISTERING,
    ACTIVE,
    SUSPECT,
    DEAD,
    RECOVERING;

    @Override
    public Set<NodeState> allowedTransitions() {
        return switch (this) {
            case REGISTERING -> Set.of(ACTIVE, DEAD);
            case ACTIVE -> Set.of(SUSPECT, DEAD);
            case SUSPECT -> Set.of(ACTIVE, DEAD);
            case DEAD -> Set.of(RECOVERING);
            case RECOVERING -> Set.of(ACTIVE, DEAD);
        };
    }
}
