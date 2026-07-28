package com.cluster.Master.topology;

import com.cluster.Master.topology.model.NodeRef;
import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.PartitionTransition;
import com.cluster.Master.topology.model.ReplicaSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves write/read owners based on partition state.
 * During transitions: writes use old-first union (old primary coordinates);
 * reads use new-first union (try new owners, fall back to old).
 * Outside transitions: both return the current replica set from PartitionRegistry.
 */
@Service
@RequiredArgsConstructor
public class DefaultReplicaMetadataService implements ReplicaMetadataService {

    private final PartitionRegistry partitionRegistry;
    private final ClusterTopology topology;

    @Override
    public ReplicaSet replicaSetForKey(String key) {
        return partitionRegistry.replicaSetForKey(key);
    }

    @Override
    public List<NodeRef> writeOwners(String key) {
        Optional<PartitionTransition> transition = activeTransition(key);
        if (transition.isPresent()) {
            PartitionTransition t = transition.get();
            // old-first union: old primary coordinates, every distinct owner written once.
            return t.oldSet().unionOwners(t.newSet());
        }
        return partitionRegistry.replicaSetForKey(key).owners();
    }

    @Override
    public List<NodeRef> readOwners(String key) {
        Optional<PartitionTransition> transition = activeTransition(key);
        if (transition.isPresent()) {
            PartitionTransition t = transition.get();
            // new-first union: try new owners, fall back to old owners until the copy finishes.
            return t.newSet().unionOwners(t.oldSet());
        }
        return partitionRegistry.replicaSetForKey(key).owners();
    }

    private Optional<PartitionTransition> activeTransition(String key) {
        long hash = topology.keyHash(key);
        return topology.migrationForHash(hash).map(Partition::transition);
    }
}
