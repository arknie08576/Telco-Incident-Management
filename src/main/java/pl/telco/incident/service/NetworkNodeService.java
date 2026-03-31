package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.List;

import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.hasNodeType;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.hasRegion;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.isActive;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.nameContains;

@Service
@RequiredArgsConstructor
public class NetworkNodeService {

    private final NetworkNodeRepository networkNodeRepository;

    @Transactional(readOnly = true)
    public List<NetworkNodeResponse> getNetworkNodes(String query, Region region, NodeType nodeType, Boolean active) {
        Specification<NetworkNode> specification = Specification
                .where(nameContains(query))
                .and(hasRegion(region))
                .and(hasNodeType(nodeType))
                .and(isActive(active));

        return networkNodeRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nodeName")).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NetworkNodeResponse mapToResponse(NetworkNode node) {
        NetworkNodeResponse response = new NetworkNodeResponse();
        response.setId(node.getId());
        response.setNodeName(node.getNodeName());
        response.setNodeType(node.getNodeType());
        response.setRegion(node.getRegion());
        response.setVendor(node.getVendor());
        response.setActive(node.getActive());
        return response;
    }
}
