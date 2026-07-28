package com.cluster.Node.replication.client;

import com.cluster.Node.replication.model.ReplicationTask;

/**
 * Transport used to deliver a replication task to a replica node. Abstracted behind an
 * interface so the REST implementation can be swapped (e.g. gRPC) without touching workers.
 */
public interface ReplicationClient {

    /**
     * Delivers the task's payload to its target replica.
     *
     * @throws ReplicationDeliveryException if the replica could not be reached or rejected it.
     */
    void send(ReplicationTask task);
}
