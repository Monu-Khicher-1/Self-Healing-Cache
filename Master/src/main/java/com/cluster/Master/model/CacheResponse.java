package com.cluster.Master.model;

import lombok.Data;

import java.sql.Timestamp;


@Data
public class CacheResponse {
    private String key;
    private String value;
    private Timestamp ttl;
}
