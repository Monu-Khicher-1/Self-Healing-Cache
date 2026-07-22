package com.cluster.Node.model.register;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;


@Data
public class RegistrationResponse {
    private String id;
    private String hostname;
    private int port;
    private Timestamp lastHeartbeat;
}
