package com.cluster.Node.model.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntry {
    private String key;
    private String value;
    /** Absolute expiry instant. Replicated as-is so replicas never drift on TTL. */
    private Timestamp expireAt;
    /** Monotonic per-key version used for last-writer-wins conflict resolution. */
    private long version;
}
