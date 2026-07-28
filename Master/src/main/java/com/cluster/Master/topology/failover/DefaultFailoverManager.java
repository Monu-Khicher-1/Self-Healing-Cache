package com.cluster.Master.topology.failover;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.service.NodeFacadeService;
import com.cluster.Master.topology.ClusterTopology;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.ReplicaSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles node failure: snapshot replica sets, evict dead node from ring,
 * promote replicas for lost primaries, and heal replica factor by reseeding new replicas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFailoverManager implements FailoverManager {

    private final ClusterTopology topology;
    private final NodeFacadeService nodeFacadeService;
    private final ReplicaPromotionManager promotionManager;
    private final ReplicaHealingService healingService;

    @Override
    public void onNodeFailed(ClusterNode node) {
        NodeRef deadRef = new NodeRef(node.getId(), node.getHostname(), node.getPort());
        int factor = topology.factor();
        HashRing hashRing = topology.ring();

        // 1. Snapshot replica sets per anchor BEFORE eviction (dead node still on the ring).
        Map<Long, List<NodeRef>> preFailureSets = new LinkedHashMap<>();
        Map<Long, Boolean> deadWasPrimary = new LinkedHashMap<>();
        for (long anchor : hashRing.ringHashes()) {
            List<NodeRef> owners = toRefs(hashRing.getNodesFromHash(anchor, factor));
            if (owners.contains(deadRef)) {
                preFailureSets.put(anchor, owners);
                deadWasPrimary.put(anchor, !owners.isEmpty() && owners.get(0).equals(deadRef));
            }
        }

        if (preFailureSets.isEmpty()) {
            log.info("Failed node {} owned no partitions; evicting only", deadRef.id());
            nodeFacadeService.evictNode(node);
            return;
        }

        log.warn("Node {} failed; affects {} partition(s). Beginning failover.",
                deadRef.id(), preFailureSets.size());

        // 2. Evict — successors become routing primaries immediately; new topology is published.
        nodeFacadeService.evictNode(node);

        // 3. Promote a healthy replica for every partition the dead node led (metadata only).
        HashRing postRing = topology.ring();
        for (Map.Entry<Long, Boolean> entry : deadWasPrimary.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            long anchor = entry.getKey();
            ReplicaSet newSet = ReplicaSet.of(
                    toRefs(postRing.getNodesFromHash(anchor, factor)), topology.version());
            promotionManager.promote(anchor, deadRef, newSet);
        }

        // 4. Heal — restore the replication factor by seeding any new replica from a live owner.
        healingService.restoreReplicationFactor(deadRef, preFailureSets);

        log.info("Failover for node {} complete; topology v{}", deadRef.id(), topology.version());
    }

    private static List<NodeRef> toRefs(List<ClusterNode> nodes) {
        List<NodeRef> refs = new ArrayList<>(nodes.size());
        for (ClusterNode n : nodes) {
            refs.add(new NodeRef(n.getId(), n.getHostname(), n.getPort()));
        }
        return refs;
    }
}
