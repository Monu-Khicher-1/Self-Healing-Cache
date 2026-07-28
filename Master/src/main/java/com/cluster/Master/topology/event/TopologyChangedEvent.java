package com.cluster.Master.topology.event;

/**
 * Published whenever the authoritative topology changes (join, leave, failure, promotion). The
 * event carries only the new version and a human-readable reason; listeners re-read the current
 * snapshot from the {@code TopologyManager}. Using Spring's event bus here is the seam that
 * decouples topology mutation from snapshot publishing and (later) migration triggering.
 */
public class TopologyChangedEvent {

    private final long version;
    private final String reason;

    public TopologyChangedEvent(long version, String reason) {
        this.version = version;
        this.reason = reason;
    }

    public long version() {
        return version;
    }

    public String reason() {
        return reason;
    }
}
