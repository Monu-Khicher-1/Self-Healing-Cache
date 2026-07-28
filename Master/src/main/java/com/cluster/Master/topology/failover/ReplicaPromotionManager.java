package com.cluster.Master.topology.failover;

import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.ReplicaSet;

/**
 * Promotes a healthy replica to primary when a primary fails. Promotion is expressed purely as a
 * {@link com.cluster.Master.topology.state.ReplicaRole} transition (REPLICA -> PROMOTING ->
 * PRIMARY) and moves <em>no data</em>: the promoted replica already holds the partition's keys via
 * ongoing replication. This mirrors how Redis Cluster promotes a replica and how Cassandra/Dynamo
 * simply treat the next healthy replica in the preference list as the coordinator.
 *
 * <p>Kept deliberately independent from migration/healing (req 9) so that leader election or
 * sync-before-serve can later be added here without touching data movement.
 */
public interface ReplicaPromotionManager {

    /**
     * Promotes the new primary of {@code newSet} in place of the failed {@code deadPrimary} for
     * the partition identified by {@code anchor}.
     *
     * @param anchor      ring position of the partition
     * @param deadPrimary the primary that failed
     * @param newSet      the recomputed replica set (its primary is the promotion target)
     */
    PromotionResult promote(long anchor, NodeRef deadPrimary, ReplicaSet newSet);
}
