package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.NetworkNodeCreateRequest;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.dto.NetworkNodeUpdateRequest;
import pl.telco.incident.mapper.NetworkNodeMapper;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.hasNodeType;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.hasRegion;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.isActive;
import static pl.telco.incident.repository.specification.NetworkNodeSpecifications.nameContains;

@Service
@RequiredArgsConstructor
public class NetworkNodeService {

    private final NetworkNodeRepository networkNodeRepository;
    private final ObservabilityEventLogger observabilityEventLogger;
    private final NetworkNodeMapper networkNodeMapper;

    @Transactional
    public NetworkNodeResponse createNetworkNode(NetworkNodeCreateRequest request) {
        String nodeName = normalize(request.getNodeName());

        if (networkNodeRepository.existsByNodeName(nodeName)) {
            throw new ConflictException("Network node with name already exists: " + nodeName);
        }

        NetworkNode node = NetworkNode.builder()
                .nodeName(nodeName)
                .nodeType(request.getNodeType())
                .region(request.getRegion())
                .vendor(normalizeNullable(request.getVendor()))
                .active(request.getActive())
                .build();

        return networkNodeMapper.toResponse(networkNodeRepository.save(node));
    }

    @Transactional
    public NetworkNodeResponse updateNetworkNode(Long id, NetworkNodeUpdateRequest request) {
        NetworkNode node = networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id));

        boolean changed = false;

        if (request.getNodeName() != null) {
            String normalizedName = normalize(request.getNodeName());
            networkNodeRepository.findByNodeName(normalizedName)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ConflictException("Network node with name already exists: " + normalizedName);
                    });

            if (!Objects.equals(node.getNodeName(), normalizedName)) {
                node.setNodeName(normalizedName);
                changed = true;
            }
        }

        if (request.getNodeType() != null && !Objects.equals(node.getNodeType(), request.getNodeType())) {
            node.setNodeType(request.getNodeType());
            changed = true;
        }

        if (request.getRegion() != null && !Objects.equals(node.getRegion(), request.getRegion())) {
            node.setRegion(request.getRegion());
            changed = true;
        }

        if (request.getVendor() != null) {
            String normalizedVendor = normalize(request.getVendor());
            if (!Objects.equals(node.getVendor(), normalizedVendor)) {
                node.setVendor(normalizedVendor);
                changed = true;
            }
        }

        if (request.getActive() != null && !Objects.equals(node.getActive(), request.getActive())) {
            node.setActive(request.getActive());
            changed = true;
        }

        if (!changed) {
            throw new BadRequestException("Patch request does not change network node");
        }

        return networkNodeMapper.toResponse(networkNodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public List<NetworkNodeResponse> getNetworkNodes(String query, Region region, NodeType nodeType, Boolean active) {
        Specification<NetworkNode> specification = Specification
                .where(nameContains(query))
                .and(hasRegion(region))
                .and(hasNodeType(nodeType))
                .and(isActive(active));

        List<NetworkNodeResponse> responses = networkNodeRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nodeName")).stream()
                .map(networkNodeMapper::toResponse)
                .toList();

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("query", query);
        fields.put("region", region);
        fields.put("nodeType", nodeType);
        fields.put("active", active);
        fields.put("resultCount", responses.size());

        observabilityEventLogger.logEvent(
                "network_node",
                "lookup",
                "search",
                "network_node_lookup",
                fields
        );

        return responses;
    }

    @Transactional(readOnly = true)
    public NetworkNodeResponse getNetworkNodeById(Long id) {
        return networkNodeMapper.toResponse(networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id)));
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }
}
