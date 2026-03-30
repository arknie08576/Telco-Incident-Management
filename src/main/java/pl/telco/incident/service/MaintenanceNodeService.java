package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.MaintenanceNodeRequest;
import pl.telco.incident.dto.MaintenanceNodeResponse;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.MaintenanceNodeRepository;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceNodeService {

    private final MaintenanceNodeRepository maintenanceNodeRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final NetworkNodeRepository networkNodeRepository;

    @Transactional(readOnly = true)
    public List<MaintenanceNodeResponse> getAllMaintenanceNodes() {
        return maintenanceNodeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceNodeResponse getMaintenanceNodeById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public MaintenanceNodeResponse createMaintenanceNode(MaintenanceNodeRequest request) {
        validateUniquePair(request.getMaintenanceWindowId(), request.getNetworkNodeId(), null);

        MaintenanceNode maintenanceNode = new MaintenanceNode();
        maintenanceNode.setMaintenanceWindow(findMaintenanceWindowByIdOrThrow(request.getMaintenanceWindowId()));
        maintenanceNode.setNetworkNode(findNetworkNodeByIdOrThrow(request.getNetworkNodeId()));

        MaintenanceNode saved = maintenanceNodeRepository.save(maintenanceNode);
        logMaintenanceNodeCrudEvent("create", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public MaintenanceNodeResponse updateMaintenanceNode(Long id, MaintenanceNodeRequest request) {
        MaintenanceNode maintenanceNode = findByIdOrThrow(id);
        validateUniquePair(request.getMaintenanceWindowId(), request.getNetworkNodeId(), id);

        maintenanceNode.setMaintenanceWindow(findMaintenanceWindowByIdOrThrow(request.getMaintenanceWindowId()));
        maintenanceNode.setNetworkNode(findNetworkNodeByIdOrThrow(request.getNetworkNodeId()));

        MaintenanceNode saved = maintenanceNodeRepository.save(maintenanceNode);
        logMaintenanceNodeCrudEvent("update", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteMaintenanceNode(Long id) {
        MaintenanceNode maintenanceNode = findByIdOrThrow(id);
        logMaintenanceNodeCrudEvent("delete", maintenanceNode);
        maintenanceNodeRepository.delete(maintenanceNode);
    }

    private MaintenanceNode findByIdOrThrow(Long id) {
        return maintenanceNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance node not found: " + id));
    }

    private MaintenanceWindow findMaintenanceWindowByIdOrThrow(Long id) {
        return maintenanceWindowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found: " + id));
    }

    private NetworkNode findNetworkNodeByIdOrThrow(Long id) {
        return networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id));
    }

    private void validateUniquePair(Long maintenanceWindowId, Long networkNodeId, Long currentId) {
        boolean exists = currentId == null
                ? maintenanceNodeRepository.existsByMaintenanceWindowIdAndNetworkNodeId(maintenanceWindowId, networkNodeId)
                : maintenanceNodeRepository.existsByMaintenanceWindowIdAndNetworkNodeIdAndIdNot(
                        maintenanceWindowId,
                        networkNodeId,
                        currentId
                );

        if (exists) {
            throw new ConflictException(
                    "Maintenance node relation already exists for maintenanceWindowId/networkNodeId: %d/%d"
                            .formatted(maintenanceWindowId, networkNodeId)
            );
        }
    }

    private MaintenanceNodeResponse mapToResponse(MaintenanceNode maintenanceNode) {
        MaintenanceNodeResponse response = new MaintenanceNodeResponse();
        response.setId(maintenanceNode.getId());
        response.setMaintenanceWindowId(maintenanceNode.getMaintenanceWindow().getId());
        response.setNetworkNodeId(maintenanceNode.getNetworkNode().getId());
        response.setCreatedAt(maintenanceNode.getCreatedAt());
        return response;
    }

    private void logMaintenanceNodeCrudEvent(String eventAction, MaintenanceNode maintenanceNode) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityId", maintenanceNode.getId());
        fields.put("maintenanceWindowId", maintenanceNode.getMaintenanceWindow().getId());
        fields.put("networkNodeId", maintenanceNode.getNetworkNode().getId());
        fields.put("createdAt", maintenanceNode.getCreatedAt());

        CrudEventLogger.log(log, "maintenance_node", eventAction, fields);
    }
}
