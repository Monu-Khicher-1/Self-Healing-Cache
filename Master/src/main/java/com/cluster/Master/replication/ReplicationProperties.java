package com.cluster.Master.replication;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cluster.replication")
public class ReplicationProperties {

    private int factor = 2;
}
