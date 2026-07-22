package com.cluster.Master.service;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.RegistrationRequest;
import com.cluster.Master.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {
    private final NodeRepository nodeRepository;

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

    public ClusterNode sendHeartbeat(String id) {
        return nodeRepository.updateHeartBeat(id);
    }

    public List<String> getExpired() {
        List<String> expiredNodes = new ArrayList<>();
        List<ClusterNode> nodes = nodeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (ClusterNode node : nodes) {

            if (node.getLastHeartbeat()
                    .toLocalDateTime()
                    .plusMinutes(4)
                    .isBefore(LocalDateTime.now())) {
                expiredNodes.add(node.getId());
            }
        }
        return expiredNodes;
    }

    public void remove(String id) {
        nodeRepository.remove(id);
    }



}
