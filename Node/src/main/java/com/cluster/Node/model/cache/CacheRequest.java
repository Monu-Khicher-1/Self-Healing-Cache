package com.cluster.Node.model.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.sql.Timestamp;


@Data
@Getter
@AllArgsConstructor
public class CacheRequest {
    private String key;
    private String value;
    private int time;
}
