package com.cluster.Master.service;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HeartBeatRequest;
import com.cluster.Master.model.RegistrationRequest;
import com.cluster.Master.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {
    private final NodeRepository nodeRepository;

    /** A node is considered failed if no heartbeat has arrived within this window (default 4 min). */
    @Value("${cluster.failure.timeout-ms:240000}")
    private long failureTimeoutMs;

    public ClusterNode findById(String id) {
        return nodeRepository.findById(id);
    }

    public boolean exists(String id) {
        return nodeRepository.exists(id);
    }

    public ClusterNode save(RegistrationRequest request) {
        ClusterNode node = ClusterNode.builder()
                .hostname(request.getHostname())
                .port(request.getPort())
                .lastHeartbeat(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        return nodeRepository.save(node);
    }

    public List<ClusterNode> findAll() {
        return nodeRepository.findAll();
    }

    public ClusterNode sendHeartbeat(HeartBeatRequest request) {
        return nodeRepository.updateHeartBeat(request);
    }

    public List<String> getExpired() {
        List<String> expiredNodes = new ArrayList<>();
        List<ClusterNode> nodes = nodeRepository.findAll();
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(failureTimeoutMs * 1_000_000);

        for (ClusterNode node : nodes) {
            if (node.getLastHeartbeat()
                    .toLocalDateTime()
                    .isBefore(cutoff)) {
                expiredNodes.add(node.getId());
            }
        }
        return expiredNodes;
    }

    public void remove(String id) {
        nodeRepository.remove(id);
    }



}
