package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentNodeResponse;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentTimelineUpsertRequest;
import pl.telco.incident.dto.IncidentUpdateRequest;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static pl.telco.incident.repository.specification.IncidentSpecifications.acknowledgedAtFrom;
import static pl.telco.incident.repository.specification.IncidentSpecifications.acknowledgedAtTo;
import static pl.telco.incident.repository.specification.IncidentSpecifications.closedAtFrom;
import static pl.telco.incident.repository.specification.IncidentSpecifications.closedAtTo;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasPossiblyPlanned;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasPriorities;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasRegion;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasSourceAlarmType;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasStatuses;
import static pl.telco.incident.repository.specification.IncidentSpecifications.incidentNumberContains;
import static pl.telco.incident.repository.specification.IncidentSpecifications.openedAtFrom;
import static pl.telco.incident.repository.specification.IncidentSpecifications.openedAtTo;
import static pl.telco.incident.repository.specification.IncidentSpecifications.resolvedAtFrom;
import static pl.telco.incident.repository.specification.IncidentSpecifications.resolvedAtTo;
import static pl.telco.incident.repository.specification.IncidentSpecifications.titleContains;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final AlarmEventRepository alarmEventRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "openedAt",
            "acknowledgedAt",
            "resolvedAt",
            "closedAt",
            "incidentNumber",
            "priority",
            "title"
    );

    private static final Set<String> NULLABLE_TIMESTAMP_SORT_FIELDS = Set.of(
            "acknowledgedAt",
            "resolvedAt",
            "closedAt"
    );

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request) {
        validateIncidentNumberUniqueness(request.getIncidentNumber());

        Long rootNodeId = request.getRootNodeId();
        List<IncidentNodeRequest> nodeRequests = request.getNodes();
        validateNodeUniqueness(nodeRequests);
        validateRootNodeConsistency(rootNodeId, nodeRequests);

        Incident incident = new Incident();
        incident.setIncidentNumber(request.getIncidentNumber().trim());
        incident.setTitle(request.getTitle().trim());
        incident.setPriority(request.getPriority());
        incident.setRegion(request.getRegion().trim());
        incident.setSourceAlarmType(normalizeNullableString(request.getSourceAlarmType()));
        incident.setPossiblyPlanned(request.getPossiblyPlanned());
        incident.setRootNode(findRootNodeByIdOrThrow(rootNodeId));

        replaceIncidentNodes(incident, nodeRequests);

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, "CREATED", "Incident created");
        logIncidentBusinessEvent("create", "CREATED", saved, null, null, false);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(
            int page,
            int size,
            String sortBy,
            String direction,
            IncidentPriority priority,
            List<String> priorities,
            String region,
            Boolean possiblyPlanned,
            IncidentStatus status,
            List<String> statuses,
            String incidentNumber,
            String title,
            String sourceAlarmType,
            LocalDateTime openedFrom,
            LocalDateTime openedTo,
            LocalDateTime acknowledgedFrom,
            LocalDateTime acknowledgedTo,
            LocalDateTime resolvedFrom,
            LocalDateTime resolvedTo,
            LocalDateTime closedFrom,
            LocalDateTime closedTo
    ) {
        validateSortBy(sortBy);
        validateDateRange("openedFrom", openedFrom, "openedTo", openedTo);
        validateDateRange("acknowledgedFrom", acknowledgedFrom, "acknowledgedTo", acknowledgedTo);
        validateDateRange("resolvedFrom", resolvedFrom, "resolvedTo", resolvedTo);
        validateDateRange("closedFrom", closedFrom, "closedTo", closedTo);

        Set<IncidentPriority> priorityFilters = mergePriorityFilters(priority, priorities);
        Set<IncidentStatus> statusFilters = mergeStatusFilters(status, statuses);
        Sort.Direction sortDirection = parseSortDirection(direction);
        Pageable pageable = PageRequest.of(page, size);

        Specification<Incident> specification = Specification
                .where(hasPriorities(priorityFilters))
                .and(hasRegion(region))
                .and(hasPossiblyPlanned(possiblyPlanned))
                .and(hasStatuses(statusFilters))
                .and(incidentNumberContains(incidentNumber))
                .and(titleContains(title))
                .and(hasSourceAlarmType(sourceAlarmType))
                .and(openedAtFrom(openedFrom))
                .and(openedAtTo(openedTo))
                .and(acknowledgedAtFrom(acknowledgedFrom))
                .and(acknowledgedAtTo(acknowledgedTo))
                .and(resolvedAtFrom(resolvedFrom))
                .and(resolvedAtTo(resolvedTo))
                .and(closedAtFrom(closedFrom))
                .and(closedAtTo(closedTo))
                .and(withSort(sortBy, sortDirection));

        return incidentRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return mapToResponse(findIncidentByIdOrThrow(id));
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Closed incidents cannot be edited");
        }

        Long effectiveRootNodeId = request.getRootNodeId() != null
                ? request.getRootNodeId()
                : incident.getRootNode() != null ? incident.getRootNode().getId() : null;
        List<IncidentNodeRequest> effectiveNodes = request.getNodes() != null
                ? request.getNodes()
                : mapCurrentNodesToRequests(incident.getIncidentNodes());

        if (request.getNodes() != null || request.getRootNodeId() != null) {
            validateNodeUniqueness(effectiveNodes);
            validateRootNodeConsistency(effectiveRootNodeId, effectiveNodes);
        }

        List<String> changedFields = new ArrayList<>();

        applyStringUpdate(
                request.getIncidentNumber(),
                incident.getIncidentNumber(),
                value -> validateIncidentNumberUniquenessForUpdate(value, id),
                incident::setIncidentNumber,
                "incidentNumber",
                changedFields
        );
        applyStringUpdate(
                request.getTitle(),
                incident.getTitle(),
                value -> {
                },
                incident::setTitle,
                "title",
                changedFields
        );
        applyObjectUpdate(request.getPriority(), incident.getPriority(), incident::setPriority, "priority", changedFields);
        applyStringUpdate(
                request.getRegion(),
                incident.getRegion(),
                value -> {
                },
                incident::setRegion,
                "region",
                changedFields
        );
        applyNullableStringUpdate(
                request.getSourceAlarmType(),
                incident.getSourceAlarmType(),
                incident::setSourceAlarmType,
                "sourceAlarmType",
                changedFields
        );
        applyObjectUpdate(
                request.getPossiblyPlanned(),
                incident.getPossiblyPlanned(),
                incident::setPossiblyPlanned,
                "possiblyPlanned",
                changedFields
        );

        if (request.getRootNodeId() != null && !Objects.equals(effectiveRootNodeId, incident.getRootNode().getId())) {
            incident.setRootNode(findRootNodeByIdOrThrow(effectiveRootNodeId));
            changedFields.add("rootNodeId");
        }

        if (request.getNodes() != null && !sameIncidentNodes(request.getNodes(), incident.getIncidentNodes())) {
            replaceIncidentNodes(incident, request.getNodes());
            incident.setRootNode(findRootNodeByIdOrThrow(effectiveRootNodeId));
            changedFields.add("nodes");
        }

        if (changedFields.isEmpty()) {
            throw new BadRequestException("Patch request does not change incident");
        }

        Incident saved = incidentRepository.save(incident);
        addTimelineEvent(saved, "UPDATED", buildUpdateMessage(changedFields));
        logIncidentBusinessEvent("update", "UPDATED", saved, null, changedFields, false);

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteIncident(Long id) {
        Incident incident = findIncidentByIdOrThrow(id);

        if (alarmEventRepository.countByIncidentId(id) > 0) {
            throw new ConflictException("Incident is referenced by alarm events and cannot be deleted: " + id);
        }

        incidentRepository.delete(incident);
        incidentRepository.flush();
        logIncidentBusinessEvent("delete", "DELETED", incident, incident.getStatus(), null, false);
    }

    @Transactional(readOnly = true)
    public List<IncidentTimelineResponse> getIncidentTimeline(Long id) {
        findIncidentByIdOrThrow(id);

        return incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(this::mapTimelineToResponse)
                .toList();
    }

    @Transactional
    public IncidentTimelineResponse createIncidentTimelineEvent(Long incidentId, IncidentTimelineUpsertRequest request) {
        Incident incident = findIncidentByIdOrThrow(incidentId);

        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(request.getEventType().trim());
        timeline.setMessage(request.getMessage().trim());

        IncidentTimeline saved = incidentTimelineRepository.save(timeline);
        logIncidentTimelineCrudEvent("create", saved, "scoped");
        return mapTimelineToResponse(saved);
    }

    @Transactional
    public IncidentTimelineResponse updateIncidentTimelineEvent(
            Long incidentId,
            Long timelineId,
            IncidentTimelineUpsertRequest request
    ) {
        findIncidentByIdOrThrow(incidentId);

        IncidentTimeline timeline = incidentTimelineRepository.findByIdAndIncidentId(timelineId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident timeline event not found: " + timelineId));

        timeline.setEventType(request.getEventType().trim());
        timeline.setMessage(request.getMessage().trim());

        IncidentTimeline saved = incidentTimelineRepository.save(timeline);
        logIncidentTimelineCrudEvent("update", saved, "scoped");
        return mapTimelineToResponse(saved);
    }

    @Transactional
    public void deleteIncidentTimelineEvent(Long incidentId, Long timelineId) {
        findIncidentByIdOrThrow(incidentId);

        IncidentTimeline timeline = incidentTimelineRepository.findByIdAndIncidentId(timelineId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident timeline event not found: " + timelineId));

        logIncidentTimelineCrudEvent("delete", timeline, "scoped");
        incidentTimelineRepository.delete(timeline);
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.OPEN, "Only OPEN incidents can be acknowledged");

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, "ACKNOWLEDGED", buildLifecycleMessage("Incident acknowledged", request));
        logIncidentBusinessEvent("acknowledge", "ACKNOWLEDGED", saved, IncidentStatus.OPEN, null, hasNote(request));

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.ACKNOWLEDGED, "Only ACKNOWLEDGED incidents can be resolved");

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, "RESOLVED", buildLifecycleMessage("Incident resolved", request));
        logIncidentBusinessEvent("resolve", "RESOLVED", saved, IncidentStatus.ACKNOWLEDGED, null, hasNote(request));

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.RESOLVED, "Only RESOLVED incidents can be closed");

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, "CLOSED", buildLifecycleMessage("Incident closed", request));
        logIncidentBusinessEvent("close", "CLOSED", saved, IncidentStatus.RESOLVED, null, hasNote(request));

        return mapToResponse(saved);
    }

    private void logIncidentBusinessEvent(
            String eventAction,
            String timelineEventType,
            Incident incident,
            IncidentStatus previousStatus,
            List<String> changedFields,
            boolean noteProvided
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("eventDataset", "incident");
        fields.put("eventCategory", "incident_management");
        fields.put("eventAction", eventAction);
        fields.put("timelineEventType", timelineEventType);
        fields.put("incidentId", incident.getId());
        fields.put("incidentNumber", incident.getIncidentNumber());
        fields.put("incidentStatus", incident.getStatus());
        fields.put("previousStatus", previousStatus);
        fields.put("priority", incident.getPriority());
        fields.put("region", incident.getRegion());
        fields.put("sourceAlarmType", incident.getSourceAlarmType());
        fields.put("possiblyPlanned", incident.getPossiblyPlanned());
        fields.put("rootNodeId", incident.getRootNode() != null ? incident.getRootNode().getId() : null);
        fields.put("nodeCount", incident.getIncidentNodes() != null ? incident.getIncidentNodes().size() : 0);
        fields.put("openedAt", incident.getOpenedAt());
        fields.put("acknowledgedAt", incident.getAcknowledgedAt());
        fields.put("resolvedAt", incident.getResolvedAt());
        fields.put("closedAt", incident.getClosedAt());
        fields.put("changedFields", changedFields);
        fields.put("noteProvided", noteProvided);

        log.info("incident_event {}", StructuredArguments.entries(fields));
    }

    private void logIncidentTimelineCrudEvent(String eventAction, IncidentTimeline timeline, String apiMode) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityId", timeline.getId());
        fields.put("incidentId", timeline.getIncident().getId());
        fields.put("eventType", timeline.getEventType());
        fields.put("message", timeline.getMessage());
        fields.put("apiMode", apiMode);
        fields.put("createdAt", timeline.getCreatedAt());

        CrudEventLogger.log(log, "incident_timeline", eventAction, fields);
    }

    private Incident findIncidentByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private NetworkNode findNetworkNodeByIdOrThrow(Long id) {
        return networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id));
    }

    private NetworkNode findRootNodeByIdOrThrow(Long id) {
        return networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Root node not found: " + id));
    }

    private void validateStatusTransition(
            Incident incident,
            IncidentStatus expectedCurrentStatus,
            String errorMessage
    ) {
        if (incident.getStatus() != expectedCurrentStatus) {
            throw new BadRequestException(errorMessage);
        }
    }

    private void addTimelineEvent(Incident incident, String eventType, String message) {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(eventType);
        timeline.setMessage(message);

        incidentTimelineRepository.save(timeline);
    }

    private String buildLifecycleMessage(String defaultMessage, IncidentActionRequest request) {
        if (!hasNote(request)) {
            return defaultMessage;
        }

        return defaultMessage + ": " + request.getNote().trim();
    }

    private boolean hasNote(IncidentActionRequest request) {
        return request != null && request.getNote() != null && !request.getNote().isBlank();
    }

    private void validateIncidentNumberUniqueness(String incidentNumber) {
        String normalizedIncidentNumber = incidentNumber.trim();

        if (incidentRepository.findByIncidentNumber(normalizedIncidentNumber).isPresent()) {
            throw new ConflictException("Incident with number already exists: " + normalizedIncidentNumber);
        }
    }

    private void validateIncidentNumberUniquenessForUpdate(String incidentNumber, Long incidentId) {
        if (incidentRepository.existsByIncidentNumberAndIdNot(incidentNumber, incidentId)) {
            throw new ConflictException("Incident with number already exists: " + incidentNumber);
        }
    }

    private void validateNodeUniqueness(List<IncidentNodeRequest> nodeRequests) {
        Set<Long> uniqueNodeIds = new HashSet<>();

        for (IncidentNodeRequest nodeRequest : nodeRequests) {
            if (!uniqueNodeIds.add(nodeRequest.getNetworkNodeId())) {
                throw new BadRequestException("Duplicate networkNodeId in nodes: " + nodeRequest.getNetworkNodeId());
            }
        }
    }

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
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

    private Specification<Incident> withSort(String sortBy, Sort.Direction sortDirection) {
        return (root, query, cb) -> {
            if (query != null && !isCountQuery(query.getResultType())) {
                if (NULLABLE_TIMESTAMP_SORT_FIELDS.contains(sortBy)) {
                    query.orderBy(
                            cb.asc(cb.selectCase().when(cb.isNull(root.get(sortBy)), 1).otherwise(0)),
                            sortDirection.isAscending() ? cb.asc(root.get(sortBy)) : cb.desc(root.get(sortBy))
                    );
                } else {
                    query.orderBy(sortDirection.isAscending() ? cb.asc(root.get(sortBy)) : cb.desc(root.get(sortBy)));
                }
            }

            return cb.conjunction();
        };
    }

    private boolean isCountQuery(Class<?> resultType) {
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }

    private void validateRootNodeConsistency(Long rootNodeId, List<IncidentNodeRequest> nodes) {
        long rootCount = nodes.stream()
                .filter(node -> node.getRole() == IncidentNodeRole.ROOT)
                .count();

        if (rootCount != 1) {
            throw new BadRequestException("Exactly one node must have role ROOT");
        }

        boolean rootMatches = nodes.stream()
                .anyMatch(node -> node.getRole() == IncidentNodeRole.ROOT && rootNodeId.equals(node.getNetworkNodeId()));

        if (!rootMatches) {
            throw new BadRequestException("rootNodeId must match the node with role ROOT");
        }
    }

    private void validateDateRange(
            String fromFieldName,
            LocalDateTime from,
            String toFieldName,
            LocalDateTime to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(fromFieldName + " must be earlier than or equal to " + toFieldName);
        }
    }

    private Set<IncidentPriority> mergePriorityFilters(IncidentPriority priority, List<String> priorities) {
        LinkedHashSet<IncidentPriority> merged = new LinkedHashSet<>();

        if (priority != null) {
            merged.add(priority);
        }
        merged.addAll(parseEnumFilters(priorities, IncidentPriority.class, "priorities"));

        return merged;
    }

    private Set<IncidentStatus> mergeStatusFilters(IncidentStatus status, List<String> statuses) {
        LinkedHashSet<IncidentStatus> merged = new LinkedHashSet<>();

        if (status != null) {
            merged.add(status);
        }
        merged.addAll(parseEnumFilters(statuses, IncidentStatus.class, "statuses"));

        return merged;
    }

    private <E extends Enum<E>> Set<E> parseEnumFilters(
            List<String> rawValues,
            Class<E> enumType,
            String parameterName
    ) {
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
                    throw new BadRequestException(
                            "Invalid value '%s' for parameter '%s'".formatted(normalizedToken, parameterName)
                    );
                }
            }
        }

        return parsedValues;
    }

    private void applyStringUpdate(
            String requestedValue,
            String currentValue,
            Consumer<String> validator,
            Consumer<String> updater,
            String fieldName,
            List<String> changedFields
    ) {
        if (requestedValue == null) {
            return;
        }

        String normalizedValue = requestedValue.trim();

        if (Objects.equals(normalizedValue, currentValue)) {
            return;
        }

        validator.accept(normalizedValue);
        updater.accept(normalizedValue);
        changedFields.add(fieldName);
    }

    private void applyNullableStringUpdate(
            String requestedValue,
            String currentValue,
            Consumer<String> updater,
            String fieldName,
            List<String> changedFields
    ) {
        if (requestedValue == null) {
            return;
        }

        String normalizedValue = normalizeNullableString(requestedValue);

        if (Objects.equals(normalizedValue, currentValue)) {
            return;
        }

        updater.accept(normalizedValue);
        changedFields.add(fieldName);
    }

    private <T> void applyObjectUpdate(
            T requestedValue,
            T currentValue,
            Consumer<T> updater,
            String fieldName,
            List<String> changedFields
    ) {
        if (requestedValue == null || Objects.equals(requestedValue, currentValue)) {
            return;
        }

        updater.accept(requestedValue);
        changedFields.add(fieldName);
    }

    private String buildUpdateMessage(List<String> changedFields) {
        return "Incident updated: " + String.join(", ", changedFields);
    }

    private void replaceIncidentNodes(Incident incident, List<IncidentNodeRequest> nodeRequests) {
        if (!incident.getIncidentNodes().isEmpty()) {
            incident.getIncidentNodes().clear();
            incidentRepository.flush();
        }

        for (IncidentNodeRequest nodeRequest : nodeRequests) {
            NetworkNode networkNode = findNetworkNodeByIdOrThrow(nodeRequest.getNetworkNodeId());

            IncidentNode incidentNode = new IncidentNode();
            incidentNode.setNetworkNode(networkNode);
            incidentNode.setRole(nodeRequest.getRole());

            incident.addIncidentNode(incidentNode);
        }
    }

    private List<IncidentNodeRequest> mapCurrentNodesToRequests(List<IncidentNode> incidentNodes) {
        return incidentNodes.stream()
                .map(incidentNode -> {
                    IncidentNodeRequest request = new IncidentNodeRequest();
                    request.setNetworkNodeId(incidentNode.getNetworkNode().getId());
                    request.setRole(incidentNode.getRole());
                    return request;
                })
                .toList();
    }

    private boolean sameIncidentNodes(List<IncidentNodeRequest> requestedNodes, List<IncidentNode> currentNodes) {
        if (requestedNodes.size() != currentNodes.size()) {
            return false;
        }

        Map<Long, IncidentNodeRole> currentNodeMap = new LinkedHashMap<>();
        for (IncidentNode currentNode : currentNodes) {
            currentNodeMap.put(currentNode.getNetworkNode().getId(), currentNode.getRole());
        }

        for (IncidentNodeRequest requestedNode : requestedNodes) {
            if (!Objects.equals(currentNodeMap.get(requestedNode.getNetworkNodeId()), requestedNode.getRole())) {
                return false;
            }
        }

        return true;
    }

    private String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private IncidentResponse mapToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setIncidentNumber(incident.getIncidentNumber());
        response.setTitle(incident.getTitle());
        response.setStatus(incident.getStatus());
        response.setPriority(incident.getPriority());
        response.setRegion(incident.getRegion());
        response.setSourceAlarmType(incident.getSourceAlarmType());
        response.setPossiblyPlanned(incident.getPossiblyPlanned());
        response.setRootNodeId(incident.getRootNode() != null ? incident.getRootNode().getId() : null);
        response.setNodes(incident.getIncidentNodes().stream()
                .map(this::mapNodeToResponse)
                .toList());
        response.setOpenedAt(incident.getOpenedAt());
        response.setAcknowledgedAt(incident.getAcknowledgedAt());
        response.setResolvedAt(incident.getResolvedAt());
        response.setClosedAt(incident.getClosedAt());
        return response;
    }

    private IncidentNodeResponse mapNodeToResponse(IncidentNode incidentNode) {
        IncidentNodeResponse response = new IncidentNodeResponse();
        response.setId(incidentNode.getId());
        response.setNetworkNodeId(incidentNode.getNetworkNode().getId());
        response.setRole(incidentNode.getRole());
        return response;
    }

    private IncidentTimelineResponse mapTimelineToResponse(IncidentTimeline timeline) {
        IncidentTimelineResponse response = new IncidentTimelineResponse();
        response.setId(timeline.getId());
        response.setEventType(timeline.getEventType());
        response.setMessage(timeline.getMessage());
        response.setCreatedAt(timeline.getCreatedAt());
        return response;
    }
}
