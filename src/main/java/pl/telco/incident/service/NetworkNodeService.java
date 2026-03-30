package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.NetworkNodeRequest;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkNodeService {

    private final NetworkNodeRepository networkNodeRepository;

    @Transactional(readOnly = true)
    public List<NetworkNodeResponse> getAllNetworkNodes() {
        return networkNodeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NetworkNodeResponse getNetworkNodeById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public NetworkNodeResponse createNetworkNode(NetworkNodeRequest request) {
        validateNodeNameUniqueness(request.getNodeName());

        NetworkNode networkNode = new NetworkNode();
        applyRequest(networkNode, request);

        NetworkNode saved = networkNodeRepository.save(networkNode);
        logNetworkNodeCrudEvent("create", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public NetworkNodeResponse updateNetworkNode(Long id, NetworkNodeRequest request) {
        NetworkNode networkNode = findByIdOrThrow(id);

        String normalizedNodeName = request.getNodeName().trim();
        if (networkNodeRepository.existsByNodeNameAndIdNot(normalizedNodeName, id)) {
            throw new ConflictException("Network node with name already exists: " + normalizedNodeName);
        }

        applyRequest(networkNode, request);

        NetworkNode saved = networkNodeRepository.save(networkNode);
        logNetworkNodeCrudEvent("update", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteNetworkNode(Long id) {
        NetworkNode networkNode = findByIdOrThrow(id);

        try {
            networkNodeRepository.delete(networkNode);
            networkNodeRepository.flush();
            logNetworkNodeCrudEvent("delete", networkNode);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Network node is still referenced and cannot be deleted: " + id);
        }
    }

    private NetworkNode findByIdOrThrow(Long id) {
        return networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id));
    }

    private void validateNodeNameUniqueness(String nodeName) {
        String normalizedNodeName = nodeName.trim();

        if (networkNodeRepository.existsByNodeName(normalizedNodeName)) {
            throw new ConflictException("Network node with name already exists: " + normalizedNodeName);
        }
    }

    private void applyRequest(NetworkNode networkNode, NetworkNodeRequest request) {
        networkNode.setNodeName(request.getNodeName().trim());
        networkNode.setNodeType(request.getNodeType());
        networkNode.setRegion(request.getRegion().trim());
        networkNode.setVendor(request.getVendor() == null ? null : request.getVendor().trim());
        networkNode.setActive(request.getActive());
    }

    private NetworkNodeResponse mapToResponse(NetworkNode networkNode) {
        NetworkNodeResponse response = new NetworkNodeResponse();
        response.setId(networkNode.getId());
        response.setNodeName(networkNode.getNodeName());
        response.setNodeType(networkNode.getNodeType());
        response.setRegion(networkNode.getRegion());
        response.setVendor(networkNode.getVendor());
        response.setActive(networkNode.getActive());
        response.setCreatedAt(networkNode.getCreatedAt());
        return response;
    }

    private void logNetworkNodeCrudEvent(String eventAction, NetworkNode networkNode) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityId", networkNode.getId());
        fields.put("nodeName", networkNode.getNodeName());
        fields.put("nodeType", networkNode.getNodeType());
        fields.put("region", networkNode.getRegion());
        fields.put("vendor", networkNode.getVendor());
        fields.put("active", networkNode.getActive());
        fields.put("createdAt", networkNode.getCreatedAt());

        CrudEventLogger.log(log, "network_node", eventAction, fields);
    }
}
