package com.cluster.Master.topology.state;

/**
 * Thrown when code attempts an illegal state transition. Carries the offending from/to states
 * so failures are self-describing in logs.
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(Enum<?> from, Enum<?> to) {
        super("Illegal state transition: " + from + " -> " + to);
    }
}
