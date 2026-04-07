package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowFilterRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.dto.MaintenanceWindowUpdateRequest;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.MaintenanceStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.endTimeFrom;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.endTimeTo;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.hasNodeId;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.hasStatuses;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.startTimeFrom;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.startTimeTo;
import static pl.telco.incident.repository.specification.MaintenanceWindowSpecifications.titleContains;

@Service
@RequiredArgsConstructor
public class MaintenanceWindowService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "title", "title",
            "status", "status",
            "startTime", "startTime",
            "endTime", "endTime"
    );

    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final NetworkNodeRepository networkNodeRepository;

    @Transactional
    public MaintenanceWindowResponse createMaintenanceWindow(MaintenanceWindowCreateRequest request) {
        List<Long> uniqueNodeIds = deduplicateNodeIds(request.getNodeIds());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        Map<Long, NetworkNode> nodesById = loadNodesById(uniqueNodeIds);

        MaintenanceWindow maintenanceWindow = new MaintenanceWindow();
        maintenanceWindow.setTitle(request.getTitle().trim());
        maintenanceWindow.setDescription(request.getDescription());
        maintenanceWindow.setStatus(request.getStatus());
        maintenanceWindow.setStartTime(request.getStartTime());
        maintenanceWindow.setEndTime(request.getEndTime());

        for (Long nodeId : uniqueNodeIds) {
            MaintenanceNode maintenanceNode = new MaintenanceNode();
            maintenanceNode.setNetworkNode(nodesById.get(nodeId));
            maintenanceWindow.addMaintenanceNode(maintenanceNode);
        }

        return mapToResponse(maintenanceWindowRepository.save(maintenanceWindow));
    }

    @Transactional
    public MaintenanceWindowResponse updateMaintenanceWindow(Long id, MaintenanceWindowUpdateRequest request) {
        MaintenanceWindow maintenanceWindow = getMaintenanceWindowEntityOrThrow(id);

        String title = maintenanceWindow.getTitle();
        String description = maintenanceWindow.getDescription();
        MaintenanceStatus status = maintenanceWindow.getStatus();
        LocalDateTime startTime = maintenanceWindow.getStartTime();
        LocalDateTime endTime = maintenanceWindow.getEndTime();
        List<Long> currentNodeIds = maintenanceWindow.getMaintenanceNodes().stream()
                .map(node -> node.getNetworkNode().getId())
                .toList();
        List<Long> requestedNodeIds = currentNodeIds;
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null) {
            String normalizedTitle = request.getTitle().trim();
            if (!normalizedTitle.equals(title)) {
                title = normalizedTitle;
                changedFields.add("title");
            }
        }

        if (request.getDescription() != null && !request.getDescription().equals(description)) {
            description = request.getDescription();
            changedFields.add("description");
        }

        if (request.getStatus() != null && request.getStatus() != status) {
            status = request.getStatus();
            changedFields.add("status");
        }

        if (request.getStartTime() != null && !request.getStartTime().equals(startTime)) {
            startTime = request.getStartTime();
            changedFields.add("startTime");
        }

        if (request.getEndTime() != null && !request.getEndTime().equals(endTime)) {
            endTime = request.getEndTime();
            changedFields.add("endTime");
        }

        if (request.getNodeIds() != null) {
            requestedNodeIds = deduplicateNodeIds(request.getNodeIds());
            loadNodesById(requestedNodeIds);

            if (!new LinkedHashSet<>(currentNodeIds).equals(new LinkedHashSet<>(requestedNodeIds))) {
                changedFields.add("nodeIds");
            }
        }

        validateTimeRange(startTime, endTime);

        if (changedFields.isEmpty()) {
            throw new BadRequestException("Patch request does not change maintenance window");
        }

        maintenanceWindow.setTitle(title);
        maintenanceWindow.setDescription(description);
        maintenanceWindow.setStatus(status);
        maintenanceWindow.setStartTime(startTime);
        maintenanceWindow.setEndTime(endTime);

        if (changedFields.contains("nodeIds")) {
            syncMaintenanceNodes(maintenanceWindow, requestedNodeIds);
        }

        return mapToResponse(maintenanceWindowRepository.save(maintenanceWindow));
    }

    @Transactional(readOnly = true)
    public MaintenanceWindowResponse getMaintenanceWindowById(Long id) {
        return mapToResponse(getMaintenanceWindowEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceWindowResponse> getMaintenanceWindows(MaintenanceWindowFilterRequest filter) {
        validateSortBy(filter.getSortBy());
        validateDateRange("startFrom", filter.getStartFrom(), "startTo", filter.getStartTo());
        validateDateRange("endFrom", filter.getEndFrom(), "endTo", filter.getEndTo());

        Set<MaintenanceStatus> statusFilters = mergeStatusFilters(filter.getStatus(), filter.getStatuses());
        Sort.Direction sortDirection = parseSortDirection(filter.getDirection());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), buildSort(filter.getSortBy(), sortDirection));

        Specification<MaintenanceWindow> specification = Specification
                .where(hasStatuses(statusFilters))
                .and(titleContains(filter.getTitle()))
                .and(hasNodeId(filter.getNodeId()))
                .and(startTimeFrom(filter.getStartFrom()))
                .and(startTimeTo(filter.getStartTo()))
                .and(endTimeFrom(filter.getEndFrom()))
                .and(endTimeTo(filter.getEndTo()));

        return maintenanceWindowRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public MaintenanceWindow getMaintenanceWindowEntityOrThrow(Long id) {
        return maintenanceWindowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found: " + id));
    }

    private MaintenanceWindowResponse mapToResponse(MaintenanceWindow maintenanceWindow) {
        MaintenanceWindowResponse response = new MaintenanceWindowResponse();
        response.setId(maintenanceWindow.getId());
        response.setTitle(maintenanceWindow.getTitle());
        response.setDescription(maintenanceWindow.getDescription());
        response.setStatus(maintenanceWindow.getStatus());
        response.setStartTime(maintenanceWindow.getStartTime());
        response.setEndTime(maintenanceWindow.getEndTime());
        response.setNodeIds(maintenanceWindow.getMaintenanceNodes().stream()
                .map(node -> node.getNetworkNode().getId())
                .toList());
        return response;
    }

    private void syncMaintenanceNodes(MaintenanceWindow maintenanceWindow, List<Long> requestedNodeIds) {
        Map<Long, MaintenanceNode> currentNodesById = maintenanceWindow.getMaintenanceNodes().stream()
                .collect(Collectors.toMap(node -> node.getNetworkNode().getId(), Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, NetworkNode> requestedNodesById = loadNodesById(requestedNodeIds);
        LinkedHashSet<Long> requestedNodeIdSet = new LinkedHashSet<>(requestedNodeIds);

        List<MaintenanceNode> nodesToRemove = maintenanceWindow.getMaintenanceNodes().stream()
                .filter(node -> !requestedNodeIdSet.contains(node.getNetworkNode().getId()))
                .toList();
        nodesToRemove.forEach(maintenanceWindow::removeMaintenanceNode);

        for (Long requestedNodeId : requestedNodeIds) {
            if (!currentNodesById.containsKey(requestedNodeId)) {
                MaintenanceNode maintenanceNode = new MaintenanceNode();
                maintenanceNode.setNetworkNode(requestedNodesById.get(requestedNodeId));
                maintenanceWindow.addMaintenanceNode(maintenanceNode);
            }
        }
    }

    private List<Long> deduplicateNodeIds(List<Long> nodeIds) {
        return new ArrayList<>(new LinkedHashSet<>(nodeIds));
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be later than startTime");
        }
    }

    private void validateDateRange(String fromFieldName, LocalDateTime from, String toFieldName, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(fromFieldName + " must be earlier than or equal to " + toFieldName);
        }
    }

    private Map<Long, NetworkNode> loadNodesById(List<Long> nodeIds) {
        Map<Long, NetworkNode> nodesById = networkNodeRepository.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(NetworkNode::getId, Function.identity()));

        if (nodesById.size() != nodeIds.size()) {
            throw new ResourceNotFoundException("One or more network nodes were not found");
        }

        return nodesById;
    }

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.containsKey(sortBy)) {
            throw new BadRequestException("Unsupported sortBy value: " + sortBy);
        }
    }

    private Sort.Direction parseSortDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported direction value: " + direction);
        }
    }

    private Set<MaintenanceStatus> mergeStatusFilters(MaintenanceStatus status, List<String> statuses) {
        LinkedHashSet<MaintenanceStatus> merged = new LinkedHashSet<>();

        if (status != null) {
            merged.add(status);
        }
        merged.addAll(parseEnumFilters(statuses, MaintenanceStatus.class, "statuses"));

        return merged;
    }

    private <E extends Enum<E>> Set<E> parseEnumFilters(List<String> rawValues, Class<E> enumType, String parameterName) {
        LinkedHashSet<E> parsedValues = new LinkedHashSet<>();

        if (rawValues == null) {
            return parsedValues;
        }

        for (String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            for (String token : rawValue.split(",")) {
                String normalizedToken = token.trim();
                if (normalizedToken.isEmpty()) {
                    continue;
                }

                try {
                    parsedValues.add(Enum.valueOf(enumType, normalizedToken.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid value '%s' for parameter '%s'".formatted(normalizedToken, parameterName));
                }
            }
        }

        return parsedValues;
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        Sort sort = Sort.by(direction, ALLOWED_SORT_FIELDS.get(sortBy));
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return sort;
    }
}
