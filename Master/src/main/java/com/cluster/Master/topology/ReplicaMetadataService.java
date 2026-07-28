package com.cluster.Master.topology;

import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.ReplicaSet;

import java.util.List;

/**
 * Single source of truth for "who owns this key" queries. All routing and failover code depends
 * on this interface rather than on the ring or replica selector directly, which is what keeps
 * HashRing, Replication, Migration and Failure Detection decoupled (req 12).
 *
 * <p>During a transition (added in a later phase) {@link #writeOwners} returns the distinct union
 * of old and new replica sets and {@link #readOwners} returns new-primary-first with old-primary
 * fallback. In this phase, with no transitions in flight, both reflect the current replica set.
 */
public interface ReplicaMetadataService {

    ReplicaSet replicaSetForKey(String key);

    /** Every distinct node a write must reach, each exactly once (union during transitions). */
    List<NodeRef> writeOwners(String key);

    /** Ordered read candidates: primary first, replicas next (plus old-primary fallback later). */
    List<NodeRef> readOwners(String key);
}
