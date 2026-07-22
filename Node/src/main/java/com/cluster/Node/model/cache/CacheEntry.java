package com.cluster.Node.model.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
public class CacheEntry {
    private String key;
    private String value;
    private Timestamp ttl;
}
