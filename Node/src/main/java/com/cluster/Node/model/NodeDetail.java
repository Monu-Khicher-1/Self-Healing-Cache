package com.cluster.Node.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeDetail {
    private String id;
    private String hostname;
    private int port;
}
