package com.cluster.Master.model;

import lombok.Data;

@Data
public class HeartBeatRequest {
    private String id;
    private String hostname;
    private int port;
}
