package com.cluster.Master.topology.state;

import java.util.Set;

/**
 * State machine for a partition's replica-set transition (join, replica replacement, etc.).
 *
 * <pre>
 * PLANNED    -> DUAL_WRITE | FAILED
 * DUAL_WRITE -> COPYING | FAILED
 * COPYING    -> VERIFYING | FAILED
 * VERIFYING  -> COMPLETED | FAILED
 * </pre>
 *
 * We enter DUAL_WRITE before COPYING so that client writes are mirrored to the new owners for
 * the entire duration of the bulk copy — no write can be lost in the copy window. This is the
 * master-coordinated analogue of Raft joint consensus (old and new configurations both active).
 */
public enum TransitionState implements State<TransitionState> {
    PLANNED,
    DUAL_WRITE,
    COPYING,
    VERIFYING,
    COMPLETED,
    FAILED;

    @Override
    public Set<TransitionState> allowedTransitions() {
        return switch (this) {
            case PLANNED -> Set.of(DUAL_WRITE, FAILED);
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
