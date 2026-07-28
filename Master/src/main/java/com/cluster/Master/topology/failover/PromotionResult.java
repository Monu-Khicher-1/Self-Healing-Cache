package com.cluster.Master.topology.failover;

import com.cluster.Master.topology.model.NodeRef;

/**
 * Outcome of a promotion attempt for one partition.
 *
 * @param anchor       ring position identifying the affected partition
 * @param deadPrimary  the primary that failed
 * @param newPrimary   the replica promoted to primary (null if the partition has no survivor)
 * @param promoted     true if a healthy replica was promoted
 */
public record PromotionResult(long anchor, NodeRef deadPrimary, NodeRef newPrimary, boolean promoted) {

    public static PromotionResult none(long anchor, NodeRef deadPrimary) {
        return new PromotionResult(anchor, deadPrimary, null, false);
    }
}
