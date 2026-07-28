package com.cluster.Node.model;

import lombok.Data;

/** A raw hash range {@code (startHash, endHash]} for internal maintenance operations. */
@Data
public class RangeRequest {
    private long startHash;
    private long endHash;
}
