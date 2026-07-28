package com.cluster.Master.topology.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable ordered set of nodes owning a partition: one primary followed by zero or more
 * replicas. The {@code version} is bumped whenever the composition changes, letting nodes and
 * (future) clients detect stale replica-set information.
 *
 * <p>Modelling the replica set as a first-class value (rather than an ad-hoc list) is what lets
 * topology changes be expressed as "old set -> new set" transitions, the way Dynamo/Cassandra
 * reason about replica placement.
 */
public final class ReplicaSet {

    private final NodeRef primary;
    private final List<NodeRef> replicas;
    private final long version;

    public ReplicaSet(NodeRef primary, List<NodeRef> replicas, long version) {
        this.primary = primary;
        this.replicas = replicas == null ? List.of() : List.copyOf(replicas);
        this.version = version;
    }

    public NodeRef primary() {
        return primary;
    }

    public List<NodeRef> replicas() {
        return replicas;
    }

    public long version() {
        return version;
    }

    /** Primary first, then replicas, de-duplicated while preserving order. */
    public List<NodeRef> owners() {
        Set<NodeRef> ordered = new LinkedHashSet<>();
        if (primary != null) {
            ordered.add(primary);
        }
        ordered.addAll(replicas);
        return List.copyOf(ordered);
    }

    /**
     * Distinct union of this replica set's owners with another's, each node appearing once and
     * preserving order (this set first). Used to compute write fan-out during a transition so a
     * node present in both old and new sets is written exactly once.
     */
    public List<NodeRef> unionOwners(ReplicaSet other) {
        Set<NodeRef> ordered = new LinkedHashSet<>(owners());
        if (other != null) {
            ordered.addAll(other.owners());
        }
        return List.copyOf(ordered);
    }

    public boolean contains(NodeRef node) {
        return owners().contains(node);
    }

    /** Builds a copy without the given node (e.g. when a member dies), preserving order. */
    public ReplicaSet without(NodeRef node, long newVersion) {
        List<NodeRef> remaining = new ArrayList<>(owners());
        remaining.remove(node);
        if (remaining.isEmpty()) {
            return new ReplicaSet(null, List.of(), newVersion);
        }
        NodeRef newPrimary = remaining.get(0);
        return new ReplicaSet(newPrimary, remaining.subList(1, remaining.size()), newVersion);
    }

    public static ReplicaSet of(List<NodeRef> orderedOwners, long version) {
        if (orderedOwners == null || orderedOwners.isEmpty()) {
            return new ReplicaSet(null, List.of(), version);
        }
        return new ReplicaSet(orderedOwners.get(0),
                orderedOwners.subList(1, orderedOwners.size()), version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplicaSet that)) return false;
        return Objects.equals(owners(), that.owners());
    }

    @Override
    public int hashCode() {
        return Objects.hash(owners());
    }

    @Override
    public String toString() {
        return "ReplicaSet{primary=" + primary + ", replicas=" + replicas + ", v=" + version + '}';
    }

    private static final ReplicaSet EMPTY = new ReplicaSet(null, Collections.emptyList(), 0);

    public static ReplicaSet empty() {
        return EMPTY;
    }
}
