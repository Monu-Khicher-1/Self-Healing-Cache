package com.cluster.Master.topology.failover;

import com.cluster.Master.topology.model.NodeRef;

import java.util.List;
import java.util.Map;

/**
 * Restores the configured replication factor after a node is lost. When a member of a partition's
 * replica set dies, that partition becomes under-replicated; a new replica (the next distinct node
 * clockwise) must be created and seeded so the factor is maintained (req 3).
 *
 * <p>Seeding always streams from a <em>surviving</em> owner directly to the new replica — never
 * from the dead node and never through an intermediary — matching Dynamo/Cassandra hinted-handoff
 * and Hazelcast backup-repair. Kept separate from {@link ReplicaPromotionManager} so promotion
 * (metadata only) and healing (data movement) evolve independently (req 9).
 */
public interface ReplicaHealingService {

    /**
     * @param deadNode        the node that failed
     * @param preFailureSets  replica sets per ring anchor captured <em>before</em> eviction, used
     *                        to detect which partitions the dead node belonged to and which new
     *                        replica has been pulled in to replace it
     */
    void restoreReplicationFactor(NodeRef deadNode, Map<Long, List<NodeRef>> preFailureSets);
}
