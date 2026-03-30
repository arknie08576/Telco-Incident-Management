package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.MaintenanceWindowRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MaintenanceWindowService {

    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final NetworkNodeRepository networkNodeRepository;

    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> getAllMaintenanceWindows() {
        return maintenanceWindowRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceWindowResponse getMaintenanceWindowById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public MaintenanceWindowResponse createMaintenanceWindow(MaintenanceWindowRequest request) {
        validateRequest(request);

        MaintenanceWindow maintenanceWindow = new MaintenanceWindow();
        applyRequest(maintenanceWindow, request);

        return mapToResponse(maintenanceWindowRepository.save(maintenanceWindow));
    }

    @Transactional
    public MaintenanceWindowResponse updateMaintenanceWindow(Long id, MaintenanceWindowRequest request) {
        validateRequest(request);

        MaintenanceWindow maintenanceWindow = findByIdOrThrow(id);
        applyRequest(maintenanceWindow, request);

        return mapToResponse(maintenanceWindowRepository.save(maintenanceWindow));
    }

    @Transactional
    public void deleteMaintenanceWindow(Long id) {
        MaintenanceWindow maintenanceWindow = findByIdOrThrow(id);
        maintenanceWindowRepository.delete(maintenanceWindow);
    }

    private MaintenanceWindow findByIdOrThrow(Long id) {
        return maintenanceWindowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found: " + id));
    }

    private void validateRequest(MaintenanceWindowRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be later than startTime");
        }

        Set<Long> uniqueNodeIds = new HashSet<>();
        for (Long networkNodeId : request.getNetworkNodeIds()) {
            if (!uniqueNodeIds.add(networkNodeId)) {
                throw new BadRequestException("Duplicate networkNodeId in maintenance nodes: " + networkNodeId);
            }
        }
    }

    private void applyRequest(MaintenanceWindow maintenanceWindow, MaintenanceWindowRequest request) {
        maintenanceWindow.setTitle(request.getTitle().trim());
        maintenanceWindow.setDescription(request.getDescription());
        maintenanceWindow.setStatus(request.getStatus());
        maintenanceWindow.setStartTime(request.getStartTime());
        maintenanceWindow.setEndTime(request.getEndTime());
        replaceMaintenanceNodes(maintenanceWindow, request.getNetworkNodeIds());
    }

    private void replaceMaintenanceNodes(MaintenanceWindow maintenanceWindow, List<Long> networkNodeIds) {
        if (!maintenanceWindow.getMaintenanceNodes().isEmpty()) {
            maintenanceWindow.getMaintenanceNodes().clear();
            maintenanceWindowRepository.flush();
        }

        for (Long networkNodeId : networkNodeIds) {
            NetworkNode networkNode = networkNodeRepository.findById(networkNodeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + networkNodeId));

            MaintenanceNode maintenanceNode = new MaintenanceNode();
            maintenanceNode.setNetworkNode(networkNode);
            maintenanceWindow.addMaintenanceNode(maintenanceNode);
        }
    }

    private MaintenanceWindowResponse mapToResponse(MaintenanceWindow maintenanceWindow) {
        MaintenanceWindowResponse response = new MaintenanceWindowResponse();
        response.setId(maintenanceWindow.getId());
        response.setTitle(maintenanceWindow.getTitle());
        response.setDescription(maintenanceWindow.getDescription());
        response.setStatus(maintenanceWindow.getStatus());
        response.setStartTime(maintenanceWindow.getStartTime());
        response.setEndTime(maintenanceWindow.getEndTime());
        response.setCreatedAt(maintenanceWindow.getCreatedAt());
        response.setNetworkNodeIds(maintenanceWindow.getMaintenanceNodes().stream()
                .map(maintenanceNode -> maintenanceNode.getNetworkNode().getId())
                .toList());
        return response;
    }
}
