package com.cluster.Master.model;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataTransferRequest {
    long startHash;
    long endHash;
    String url;
}