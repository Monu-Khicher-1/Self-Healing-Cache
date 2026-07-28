package com.cluster.Master.topology.model;

/**
 * A contiguous arc of the hash ring together with the replica set that owns it and, when a
 * topology change is in flight, its {@link PartitionTransition}. Ranges follow the existing ring
 * convention: the owning primary sits at {@code rangeStart} and owns keys up to (but not
 * including) the next node at {@code rangeEnd}.
 *
 * <p>Introducing an explicit Partition (like Cassandra token ranges / Hazelcast partitions) is
 * what allows ownership and transitions to be reasoned about per-range instead of per-key.
 */
public final class Partition {

    private final String id;
    private final long rangeStart;
    private final long rangeEnd;
    private volatile ReplicaSet current;
    private volatile PartitionTransition transition;

    public Partition(String id, long rangeStart, long rangeEnd, ReplicaSet current) {
        this.id = id;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.current = current;
    }

    public String id() {
        return id;
    }

    public long rangeStart() {
        return rangeStart;
    }

    public long rangeEnd() {
        return rangeEnd;
    }

    public ReplicaSet current() {
        return current;
    }

    public void setCurrent(ReplicaSet current) {
        this.current = current;
    }

    public PartitionTransition transition() {
        return transition;
    }

    public void setTransition(PartitionTransition transition) {
        this.transition = transition;
    }

    public boolean isTransitioning() {
        return transition != null && transition.isActive();
    }

    /**
     * True if {@code hash} falls in this partition's range under the ring's floor() ownership.
     * Handles the wrap-around case where the range spans the end of the ring (rangeStart >
     * rangeEnd), matching the node-side {@code (start, end]} range matcher.
     */
    public boolean covers(long hash) {
        if (rangeStart <= rangeEnd) {
            return hash >= rangeStart && hash <= rangeEnd;
        }
        return hash >= rangeStart || hash <= rangeEnd;
    }
}
