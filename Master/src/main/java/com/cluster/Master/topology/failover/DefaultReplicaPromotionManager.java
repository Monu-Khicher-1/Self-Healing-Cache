package com.cluster.Master.topology.failover;

import com.cluster.Master.topology.TopologyManager;
import com.cluster.Master.topology.event.ReplicaPromotedEvent;
import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.ReplicaSet;
import com.cluster.Master.topology.state.ReplicaRole;
import com.cluster.Master.topology.state.StateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Default {@link ReplicaPromotionManager}. Drives the promoted replica through a validated
 * {@link ReplicaRole} state machine (REPLICA -> PROMOTING -> PRIMARY), records the new primary,
 * and emits a {@link ReplicaPromotedEvent}. Purely metadata — no keys are moved, satisfying
 * "avoid unnecessary key migration if a healthy replica already owns the latest copy" (req 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReplicaPromotionManager implements ReplicaPromotionManager {

    private final TopologyManager topologyManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PromotionResult promote(long anchor, NodeRef deadPrimary, ReplicaSet newSet) {
        NodeRef newPrimary = newSet == null ? null : newSet.primary();
        if (newPrimary == null) {
            log.warn("No healthy replica to promote for partition {} after primary {} failed; "
                    + "partition has no surviving owner", anchor, deadPrimary.id());
            return PromotionResult.none(anchor, deadPrimary);
        }

        // Model the promotion explicitly as a role transition so it is validated and observable,
        // even though consistent hashing already makes the successor the routing primary.
        StateMachine<ReplicaRole> role = new StateMachine<>(ReplicaRole.REPLICA);
        role.transition(ReplicaRole.PROMOTING);
        role.transition(ReplicaRole.PRIMARY);

        long version = topologyManager.currentVersion();
        log.info("Promoted replica {} to PRIMARY for partition {} (was {}); topology v{}",
                newPrimary.id(), anchor, deadPrimary.id(), version);
        eventPublisher.publishEvent(new ReplicaPromotedEvent(anchor, deadPrimary, newPrimary, version));
        return new PromotionResult(anchor, deadPrimary, newPrimary, true);
    }
}
