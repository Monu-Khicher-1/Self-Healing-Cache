package com.cluster.Master.replication;

import com.cluster.Master.topology.ReplicaMetadataService;
import com.cluster.Master.topology.model.NodeRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read/write target provider for the router. Delegates ownership resolution to the topology
 * {@link ReplicaMetadataService}, so routing automatically reflects promotions and (later)
 * in-flight replica-set transitions without any change here.
 *
 * <p>This is the designated seam for future <em>replica promotion</em>: once the metadata service
 * reflects a promoted replica as primary, reads and writes follow with no router change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailureRecoveryManager {

    private final ReplicaMetadataService replicaMetadataService;

    /**
     * Ordered live nodes to try for a read: primary first, then replicas (plus the old primary as
     * a fallback during a transition). The router attempts each in turn and returns the first hit.
     */
    public List<NodeRef> getReadCandidates(String key) {
        List<NodeRef> candidates = replicaMetadataService.readOwners(key);
        if (candidates.isEmpty()) {
            log.warn("No live nodes available to serve key {}", key);
        }
        return candidates;
    }

    /**
     * Every distinct node a write must reach: the current owners, unioned with incoming owners
     * during a transition, each written exactly once. Index 0 is the primary.
     */
    public List<NodeRef> getWriteTargets(String key) {
        return replicaMetadataService.writeOwners(key);
    }
}
