package com.cluster.Master.topology.state;

import java.util.Set;

/**
 * Contract for an enum that participates in a validated state machine. Each constant declares
 * the set of states it may legally move to, so illegal transitions can be rejected centrally
 * rather than scattered across the codebase.
 *
 * @param <S> the concrete enum type
 */
public interface State<S extends Enum<S>> {
    Set<S> allowedTransitions();
}
