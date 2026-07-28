package com.cluster.Master.topology.failover;

import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.router.DataTransferRouter;
import com.cluster.Master.topology.ClusterTopology;
import com.cluster.Master.topology.model.NodeRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default {@link ReplicaHealingService}. For every partition the dead node belonged to, it
 * recomputes the replica set from the post-eviction ring and, if a brand-new owner has been pulled
 * in, streams that partition's key range to the new owner from a surviving owner (the new primary).
 *
 * <p>Placement parity: the post-failure replica set is computed with the same
 * {@link HashRing#getNodesFromHash(long, int)} walk used for routing, so the node being seeded is
 * exactly the node routing will send future writes to. Seeding is idempotent because the sink
 * applies entries under version-gating, so it is safe even if a concurrent write races the copy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReplicaHealingService implements ReplicaHealingService {

    private final ClusterTopology topology;
    private final DataTransferRouter dataTransferRouter;

    @Override
    public void restoreReplicationFactor(NodeRef deadNode, Map<Long, List<NodeRef>> preFailureSets) {
        int factor = topology.factor();
        HashRing hashRing = topology.ring();
        for (Map.Entry<Long, List<NodeRef>> entry : preFailureSets.entrySet()) {
            long anchor = entry.getKey();
            List<NodeRef> oldOwners = entry.getValue();
            if (!oldOwners.contains(deadNode)) {
                continue; // dead node was not part of this partition
            }

            // The dead node's own anchor is removed from the ring, but getNodesFromHash walks from
            // floor(anchor) so it still resolves the new owners for that key range; no special case
            // is needed here.
            List<NodeRef> newOwners = toRefs(hashRing.getNodesFromHash(anchor, factor));

            // Seed from a SURVIVING owner that already holds this partition's data (a node present
            // in both the old and new sets). The new primary may itself be a freshly-introduced
            // owner with no data, so it cannot be the source.
            NodeRef source = null;
            for (NodeRef owner : newOwners) {
                if (oldOwners.contains(owner)) {
                    source = owner;
                    break;
                }
            }
            if (source == null) {
                log.warn("No surviving owner holds partition {}; cannot heal (potential data loss)", anchor);
                continue;
            }

            // Key range owned by this partition is [anchor, successor) under floor() ownership.
            // The node's transfer matches (startHash, endHash] and natively handles wrap-around
            // (startHash > endHash), so we pass startHash = anchor - 1, endHash = successor - 1.
            long successor = hashRing.successorHash(anchor);
            long startHash = anchor - 1;
            long endHash = successor - 1;

            for (NodeRef owner : newOwners) {
                if (oldOwners.contains(owner)) {
                    continue; // already held a copy before the failure
                }
                log.info("Healing partition {} (range ({},{}]): seeding new replica {} from live owner {}",
                        anchor, startHash, endHash, owner.id(), source.id());
                dataTransferRouter.notifyTransferKeys(source, owner, startHash, endHash);
            }
        }
    }

    private static List<NodeRef> toRefs(List<ClusterNode> nodes) {
        List<NodeRef> refs = new ArrayList<>(nodes.size());
        for (ClusterNode node : nodes) {
            refs.add(new NodeRef(node.getId(), node.getHostname(), node.getPort()));
        }
        return refs;
    }
}
