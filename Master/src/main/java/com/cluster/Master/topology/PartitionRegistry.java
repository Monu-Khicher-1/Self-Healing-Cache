package com.cluster.Master.topology;

import com.cluster.Master.topology.model.Partition;
import com.cluster.Master.topology.model.ReplicaSet;

import java.util.List;

/**
 * Derives partitions and replica sets from the current ring. This is the only place that turns
 * raw ring geometry into topology domain objects, keeping {@code HashRing} a dumb structure and
 * the rest of the system dependent on the {@link ReplicaSet} abstraction rather than ring math.
 */
public interface PartitionRegistry {

    /** Replica set (primary + replicas) responsible for a key, per the current ring + factor. */
    ReplicaSet replicaSetForKey(String key);

    /** One partition per ring node, each carrying its range and current replica set. */
    List<Partition> partitions();
}
