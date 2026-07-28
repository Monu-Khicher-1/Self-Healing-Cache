package com.cluster.Master.topology.state;

/**
 * Generic, thread-safe state machine over a {@link State} enum. Centralises transition
 * validation so every stateful object (node, replica role, partition transition) enforces its
 * legal moves the same way (single-responsibility, open for new enums without modification).
 *
 * @param <S> the enum type driving this machine
 */
public final class StateMachine<S extends Enum<S> & State<S>> {

    private volatile S current;

    public StateMachine(S initial) {
        this.current = initial;
    }

    public S current() {
        return current;
    }

    public boolean canTransition(S next) {
        return current.allowedTransitions().contains(next);
    }

    /**
     * Moves to {@code next} if the transition is legal.
     *
     * @throws IllegalStateTransitionException if the move is not allowed from the current state
     */
    public synchronized S transition(S next) {
        if (!current.allowedTransitions().contains(next)) {
            throw new IllegalStateTransitionException(current, next);
        }
        this.current = next;
        return current;
    }
}
