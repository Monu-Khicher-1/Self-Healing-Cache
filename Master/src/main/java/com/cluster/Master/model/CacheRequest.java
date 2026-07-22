package com.cluster.Master.model;


import lombok.Data;

@Data
public class CacheRequest {
    private String key;
    private String value;
    private int time;
}