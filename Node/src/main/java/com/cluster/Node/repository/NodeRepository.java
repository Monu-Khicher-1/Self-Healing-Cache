package com.cluster.Node.repository;


import com.cluster.Node.model.NodeDetail;
import com.cluster.Node.model.register.RegistrationResponse;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@NoArgsConstructor
public class NodeRepository {
    private NodeDetail node;

    public void setNode(RegistrationResponse response){
        node = new NodeDetail(response.getId(),response.getHostname(), response.getPort());
    }

    public String getNodeId(){
        if(node == null) return null;
        return node.getId();
    }
}
