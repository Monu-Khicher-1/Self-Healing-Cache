package com.cluster.Node.topology;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the most recent {@link TopologySnapshot} this node has received from the master. Updates
 * are guarded by the monotonically increasing topology version, so an out-of-order or duplicate
 * push can never move the node's view backwards (the same version-gating idea used for cache
 * entries). This is the foundation for future client-side / node-local routing.
 */
@Slf4j
@Component
public class LocalTopologyView {

    private final AtomicReference<TopologySnapshot> current = new AtomicReference<>();

    /**
     * Applies a snapshot iff its version is strictly newer than the one held.
     *
     * @return true if the snapshot was accepted and installed.
     */
    public boolean apply(TopologySnapshot snapshot) {
        while (true) {
            TopologySnapshot existing = current.get();
            if (existing != null && snapshot.version() <= existing.version()) {
                log.debug("Ignoring stale topology v{} (current v{})",
                        snapshot.version(), existing.version());
                return false;
            }
            if (current.compareAndSet(existing, snapshot)) {
                log.info("Installed topology v{} with {} node(s), {} partition(s)",
                        snapshot.version(), snapshot.nodes().size(), snapshot.partitions().size());
                return true;
            }
        }
    }

    public TopologySnapshot current() {
        return current.get();
    }

    public long currentVersion() {
        TopologySnapshot snapshot = current.get();
        return snapshot == null ? 0L : snapshot.version();
    }
}
