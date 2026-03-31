package pl.telco.incident.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import pl.telco.incident.dto.IncidentSummaryResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentUpdateRequest;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.Duration;
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
import java.util.stream.Collectors;

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

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request) {
        validateIncidentNumberUniqueness(normalize(request.getIncidentNumber()));
        validateNodeUniqueness(request);
        validateRootNodeConsistency(request);

        Map<Long, NetworkNode> nodesById = loadRequestedNodes(request);
        NetworkNode rootNode = nodesById.get(request.getRootNodeId());

        if (rootNode == null) {
            throw new ResourceNotFoundException("Root node not found: " + request.getRootNodeId());
        }

        Incident incident = new Incident();
        incident.setIncidentNumber(normalize(request.getIncidentNumber()));
        incident.setTitle(normalize(request.getTitle()));
        incident.setPriority(request.getPriority());
        incident.setRegion(request.getRegion());
        incident.setSourceAlarmType(request.getSourceAlarmType());
        incident.setPossiblyPlanned(request.getPossiblyPlanned());
        incident.setRootNode(rootNode);

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            NetworkNode networkNode = nodesById.get(nodeRequest.getNetworkNodeId());

            if (networkNode == null) {
                throw new ResourceNotFoundException("Network node not found: " + nodeRequest.getNetworkNodeId());
            }

            IncidentNode incidentNode = new IncidentNode();
            incidentNode.setNetworkNode(networkNode);
            incidentNode.setRole(nodeRequest.getRole());
            incident.addIncidentNode(incidentNode);
        }

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, IncidentTimelineEventType.CREATED, "Incident created");
        recordIncidentCreated(saved);
        logIncidentBusinessEvent("create", IncidentTimelineEventType.CREATED, saved, null, null, false);

        return mapToDetailedResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getAllIncidents(
            int page,
            int size,
            String sortBy,
            String direction,
            IncidentPriority priority,
            List<String> priorities,
            Region region,
            Boolean possiblyPlanned,
            IncidentStatus status,
            List<String> statuses,
            String incidentNumber,
            String title,
            SourceAlarmType sourceAlarmType,
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
                .map(this::mapToSummaryResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return mapToDetailedResponse(findDetailedIncidentByIdOrThrow(id));
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request) {
        Incident incident = findDetailedIncidentByIdOrThrow(id);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Closed incidents cannot be edited");
        }

        List<String> changedFields = new ArrayList<>();

        applyStringUpdate(
                request.getIncidentNumber(),
                incident.getIncidentNumber(),
                value -> validateIncidentNumberUniquenessForUpdate(value, incident.getId()),
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
        applyObjectUpdate(request.getRegion(), incident.getRegion(), incident::setRegion, "region", changedFields);
        applyObjectUpdate(
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
        applyIncidentNodeUpdate(request, incident, changedFields);

        if (changedFields.isEmpty()) {
            throw new BadRequestException("Patch request does not change incident");
        }

        Incident saved = incidentRepository.save(incident);
        addTimelineEvent(saved, IncidentTimelineEventType.UPDATED, buildUpdateMessage(changedFields));

        recordIncidentUpdated(saved, changedFields);
        logIncidentBusinessEvent("update", IncidentTimelineEventType.UPDATED, saved, null, changedFields, false);

        return mapToDetailedResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentTimelineResponse> getIncidentTimeline(Long id) {
        findIncidentByIdOrThrow(id);

        return incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(this::mapTimelineToResponse)
                .toList();
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.OPEN, "Only OPEN incidents can be acknowledged");

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, IncidentTimelineEventType.ACKNOWLEDGED, buildLifecycleMessage("Incident acknowledged", request));
        recordLifecycleTransition("acknowledge", IncidentTimelineEventType.ACKNOWLEDGED, saved);
        logIncidentBusinessEvent("acknowledge", IncidentTimelineEventType.ACKNOWLEDGED, saved, IncidentStatus.OPEN, null, hasNote(request));

        return mapToDetailedResponse(saved);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.ACKNOWLEDGED, "Only ACKNOWLEDGED incidents can be resolved");

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, IncidentTimelineEventType.RESOLVED, buildLifecycleMessage("Incident resolved", request));
        recordLifecycleTransition("resolve", IncidentTimelineEventType.RESOLVED, saved);
        logIncidentBusinessEvent("resolve", IncidentTimelineEventType.RESOLVED, saved, IncidentStatus.ACKNOWLEDGED, null, hasNote(request));

        return mapToDetailedResponse(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(incident, IncidentStatus.RESOLVED, "Only RESOLVED incidents can be closed");

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(saved, IncidentTimelineEventType.CLOSED, buildLifecycleMessage("Incident closed", request));
        recordLifecycleTransition("close", IncidentTimelineEventType.CLOSED, saved);
        logIncidentBusinessEvent("close", IncidentTimelineEventType.CLOSED, saved, IncidentStatus.RESOLVED, null, hasNote(request));

        return mapToDetailedResponse(saved);
    }

    private void recordIncidentCreated(Incident incident) {
        meterRegistry.counter(
                "incident.created",
                "priority", incident.getPriority().name(),
                "region", incident.getRegion().name()
        ).increment();
    }

    private void recordIncidentUpdated(Incident incident, List<String> changedFields) {
        meterRegistry.counter(
                "incident.updated",
                "priority", incident.getPriority().name(),
                "region", incident.getRegion().name()
        ).increment();

        meterRegistry.summary("incident.updated.changed_fields").record(changedFields.size());
    }

    private void recordLifecycleTransition(String action, IncidentTimelineEventType eventType, Incident incident) {
        meterRegistry.counter(
                "incident.lifecycle.transition",
                "action", action,
                "eventType", eventType.name(),
                "priority", incident.getPriority().name(),
                "region", incident.getRegion().name(),
                "status", incident.getStatus().name()
        ).increment();

        if (incident.getOpenedAt() == null) {
            return;
        }

        switch (eventType) {
            case ACKNOWLEDGED -> recordDuration("incident.time.to_ack", incident.getOpenedAt(), incident.getAcknowledgedAt(), incident);
            case RESOLVED -> recordDuration("incident.time.to_resolve", incident.getOpenedAt(), incident.getResolvedAt(), incident);
            case CLOSED -> recordDuration("incident.time.to_close", incident.getOpenedAt(), incident.getClosedAt(), incident);
            default -> {
            }
        }
    }

    private void recordDuration(String metricName, LocalDateTime startedAt, LocalDateTime finishedAt, Incident incident) {
        if (startedAt == null || finishedAt == null || finishedAt.isBefore(startedAt)) {
            return;
        }

        Timer.builder(metricName)
                .tags(
                        "priority", incident.getPriority().name(),
                        "region", incident.getRegion().name()
                )
                .register(meterRegistry)
                .record(Duration.between(startedAt, finishedAt));
    }

    private void logIncidentBusinessEvent(
            String eventAction,
            IncidentTimelineEventType timelineEventType,
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

    private Incident findDetailedIncidentByIdOrThrow(Long id) {
        return incidentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private Incident findIncidentByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private Map<Long, NetworkNode> loadRequestedNodes(IncidentCreateRequest request) {
        return loadRequestedNodes(request.getRootNodeId(), request.getNodes());
    }

    private void validateStatusTransition(Incident incident, IncidentStatus expectedCurrentStatus, String errorMessage) {
        if (incident.getStatus() != expectedCurrentStatus) {
            throw new BadRequestException(errorMessage);
        }
    }

    private void addTimelineEvent(Incident incident, IncidentTimelineEventType eventType, String message) {
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
        if (incidentRepository.findByIncidentNumber(incidentNumber).isPresent()) {
            throw new ConflictException("Incident with number already exists: " + incidentNumber);
        }
    }

    private void validateIncidentNumberUniquenessForUpdate(String incidentNumber, Long incidentId) {
        incidentRepository.findByIncidentNumber(incidentNumber)
                .filter(existingIncident -> !existingIncident.getId().equals(incidentId))
                .ifPresent(existingIncident -> {
                    throw new ConflictException("Incident with number already exists: " + incidentNumber);
                });
    }

    private void validateNodeUniqueness(IncidentCreateRequest request) {
        validateNodeUniqueness(request.getNodes());
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

    private void validateRootNodeConsistency(IncidentCreateRequest request) {
        validateRootNodeConsistency(request.getNodes(), request.getRootNodeId());
    }

    private void validateRootNodeConsistency(List<IncidentNodeRequest> nodeRequests, Long rootNodeId) {
        long rootCount = nodeRequests.stream()
                .filter(node -> node.getRole() == IncidentNodeRole.ROOT)
                .count();

        if (rootCount != 1) {
            throw new BadRequestException("Exactly one node must have role ROOT");
        }

        boolean rootMatches = nodeRequests.stream()
                .anyMatch(node -> node.getRole() == IncidentNodeRole.ROOT && rootNodeId.equals(node.getNetworkNodeId()));

        if (!rootMatches) {
            throw new BadRequestException("rootNodeId must match the node with role ROOT");
        }
    }

    private void applyIncidentNodeUpdate(IncidentUpdateRequest request, Incident incident, List<String> changedFields) {
        if (request.getRootNodeId() != null && request.getNodes() == null) {
            throw new BadRequestException("rootNodeId can only be updated together with nodes");
        }

        if (request.getNodes() == null) {
            return;
        }

        validateNodeUniqueness(request.getNodes());

        Long requestedRootNodeId = resolveRequestedRootNodeId(request);
        validateRootNodeConsistency(request.getNodes(), requestedRootNodeId);

        Map<Long, NetworkNode> nodesById = loadRequestedNodes(requestedRootNodeId, request.getNodes());
        NetworkNode requestedRootNode = nodesById.get(requestedRootNodeId);

        if (requestedRootNode == null) {
            throw new ResourceNotFoundException("Root node not found: " + requestedRootNodeId);
        }

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            if (!nodesById.containsKey(nodeRequest.getNetworkNodeId())) {
                throw new ResourceNotFoundException("Network node not found: " + nodeRequest.getNetworkNodeId());
            }
        }

        Map<Long, IncidentNodeRole> currentAssignments = incident.getIncidentNodes().stream()
                .collect(Collectors.toMap(node -> node.getNetworkNode().getId(), IncidentNode::getRole));
        Map<Long, IncidentNodeRole> requestedAssignments = request.getNodes().stream()
                .collect(Collectors.toMap(IncidentNodeRequest::getNetworkNodeId, IncidentNodeRequest::getRole));

        boolean rootChanged = !Objects.equals(
                incident.getRootNode() != null ? incident.getRootNode().getId() : null,
                requestedRootNodeId
        );
        boolean nodesChanged = !currentAssignments.equals(requestedAssignments);

        if (!rootChanged && !nodesChanged) {
            return;
        }

        incident.setRootNode(requestedRootNode);
        if (nodesChanged) {
            incident.getIncidentNodes().clear();
            incidentRepository.flush();

            for (IncidentNodeRequest nodeRequest : request.getNodes()) {
                IncidentNode incidentNode = new IncidentNode();
                incidentNode.setNetworkNode(nodesById.get(nodeRequest.getNetworkNodeId()));
                incidentNode.setRole(nodeRequest.getRole());
                incident.addIncidentNode(incidentNode);
            }
        }

        if (rootChanged) {
            changedFields.add("rootNodeId");
        }
        if (nodesChanged) {
            changedFields.add("nodes");
        }
    }

    private Long resolveRequestedRootNodeId(IncidentUpdateRequest request) {
        if (request.getRootNodeId() != null) {
            return request.getRootNodeId();
        }

        return request.getNodes().stream()
                .filter(node -> node.getRole() == IncidentNodeRole.ROOT)
                .map(IncidentNodeRequest::getNetworkNodeId)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Exactly one node must have role ROOT"));
    }

    private Map<Long, NetworkNode> loadRequestedNodes(Long rootNodeId, List<IncidentNodeRequest> nodeRequests) {
        Set<Long> requestedNodeIds = nodeRequests.stream()
                .map(IncidentNodeRequest::getNetworkNodeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (rootNodeId != null) {
            requestedNodeIds.add(rootNodeId);
        }

        return networkNodeRepository.findAllById(requestedNodeIds).stream()
                .collect(Collectors.toMap(NetworkNode::getId, node -> node));
    }

    private void validateDateRange(String fromFieldName, LocalDateTime from, String toFieldName, LocalDateTime to) {
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

        String normalizedValue = normalize(requestedValue);

        if (Objects.equals(normalizedValue, currentValue)) {
            return;
        }

        validator.accept(normalizedValue);
        updater.accept(normalizedValue);
        changedFields.add(fieldName);
    }

    private <T> void applyObjectUpdate(T requestedValue, T currentValue, Consumer<T> updater, String fieldName, List<String> changedFields) {
        if (requestedValue == null || Objects.equals(requestedValue, currentValue)) {
            return;
        }

        updater.accept(requestedValue);
        changedFields.add(fieldName);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String buildUpdateMessage(List<String> changedFields) {
        return "Incident updated: " + String.join(", ", changedFields);
    }

    private IncidentSummaryResponse mapToSummaryResponse(Incident incident) {
        IncidentSummaryResponse response = new IncidentSummaryResponse();
        response.setId(incident.getId());
        response.setIncidentNumber(incident.getIncidentNumber());
        response.setTitle(incident.getTitle());
        response.setStatus(incident.getStatus());
        response.setPriority(incident.getPriority());
        response.setRegion(incident.getRegion());
        response.setOpenedAt(incident.getOpenedAt());
        response.setAcknowledgedAt(incident.getAcknowledgedAt());
        response.setResolvedAt(incident.getResolvedAt());
        response.setClosedAt(incident.getClosedAt());
        return response;
    }

    private IncidentResponse mapToDetailedResponse(Incident incident) {
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
        response.setOpenedAt(incident.getOpenedAt());
        response.setAcknowledgedAt(incident.getAcknowledgedAt());
        response.setResolvedAt(incident.getResolvedAt());
        response.setClosedAt(incident.getClosedAt());
        response.setNodes(incident.getIncidentNodes().stream().map(this::mapIncidentNodeToResponse).toList());
        return response;
    }

    private IncidentNodeResponse mapIncidentNodeToResponse(IncidentNode incidentNode) {
        IncidentNodeResponse response = new IncidentNodeResponse();
        response.setNetworkNodeId(incidentNode.getNetworkNode().getId());
        response.setNodeName(incidentNode.getNetworkNode().getNodeName());
        response.setRole(incidentNode.getRole());
        response.setNodeType(incidentNode.getNetworkNode().getNodeType());
        response.setRegion(incidentNode.getNetworkNode().getRegion());
        response.setVendor(incidentNode.getNetworkNode().getVendor());
        response.setActive(incidentNode.getNetworkNode().getActive());
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
