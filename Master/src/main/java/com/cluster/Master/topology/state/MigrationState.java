package com.cluster.Master.topology.state;

import java.util.Set;

/**
 * State machine for an individual migration task (one source streaming a range to one or more
 * destinations). Distinct from {@link TransitionState}, which tracks the partition as a whole:
 * a partition transition may aggregate several migration tasks. Mirrors the transition stages
 * so a coordinator can roll a partition forward only once every task reaches COMPLETED.
 */
public enum MigrationState implements State<MigrationState> {
    PLANNED,
    COPYING,
    DUAL_WRITE,
    VERIFYING,
    COMPLETED,
    FAILED;

    @Override
    public Set<MigrationState> allowedTransitions() {
        return switch (this) {
            case PLANNED -> Set.of(DUAL_WRITE, COPYING, FAILED);
            case DUAL_WRITE -> Set.of(COPYING, FAILED);
            case COPYING -> Set.of(VERIFYING, FAILED);
            case VERIFYING -> Set.of(COMPLETED, FAILED);
            case COMPLETED, FAILED -> Set.of();
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
