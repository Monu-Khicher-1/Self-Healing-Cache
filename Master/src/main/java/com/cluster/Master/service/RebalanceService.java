package com.cluster.Master.service;


import com.cluster.Master.model.ClusterNode;
import com.cluster.Master.model.HashRing;
import com.cluster.Master.router.DataTransferRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RebalanceService {
    private final DataTransferRouter dataTransferRouter;
    private final HashRing hashRing;

    public void rebalanceKeysForAddition(ClusterNode newNode) {
        ClusterNode successorNode = hashRing.getNode(newNode);
        if (successorNode == null) {
            return;
        }
        log.info("Rebalancing keys from node {} to new node {}", successorNode.getId(), newNode.getId());
        long startHash = hashRing.startHash(newNode);
        long endHash = hashRing.endHash(newNode);
        if (startHash == endHash) {
            log.info("No keys to rebalance for node {}", newNode.getId());
            return;
        }
        log.info("Transferring keys in range [{} - {}] from node {} to new node {}", startHash, endHash, successorNode.getId(), newNode.getId());
        dataTransferRouter.notifyTransferKeys(successorNode, newNode, startHash, endHash);

    }

    public void rebalanceKeysForRemoval(ClusterNode node) {
        ClusterNode successorNode = hashRing.getNode(node);
        if (successorNode == null) {
            return;
        }
        log.info("Rebalancing keys from node {} to node {}", node.getId(), successorNode.getId());
        long startHash = hashRing.startHash(node);
        long endHash = hashRing.endHash(node);
        if (startHash == endHash) {
            log.info("No keys to rebalance for node {}", node.getId());
            return;
        }
        log.info("Transferring keys in range [{} - {}] from node {} to node {}", startHash, endHash,  node.getId(), successorNode.getId());
        dataTransferRouter.notifyTransferKeys(node, successorNode, startHash, endHash);

    }


}
