package com.cluster.Node.model.register;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationRequest {
    private String hostname;
    private int port;
}
