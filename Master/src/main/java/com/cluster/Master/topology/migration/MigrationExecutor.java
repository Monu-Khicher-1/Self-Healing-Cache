package com.cluster.Master.topology.migration;

import com.cluster.Master.topology.model.Partition;

/**
 * Performs the bulk data copy for a single partition transition. Streams the partition's key range
 * <em>directly</em> from the old primary to every newly-introduced owner (req 6/21: source streams
 * straight to each destination, never through the master or an intermediate node), reusing the node
 * {@code /send-data} -> {@code /receive-data} path. Copy is idempotent because the sink applies
 * entries under version-gating.
 */
public interface MigrationExecutor {

    /**
     * Copies {@code partition}'s range from its transition's old primary to each new owner that did
     * not already hold the data.
     *
     * @return number of destination nodes seeded
     */
    int copy(Partition partition);
}
