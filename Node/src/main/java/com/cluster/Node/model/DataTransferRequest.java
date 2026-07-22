package com.cluster.Node.model;


import lombok.Data;

@Data
public class DataTransferRequest {
    long startHash;
    long endHash;
    String url;
}
