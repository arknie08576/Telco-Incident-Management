package pl.telco.incident.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentFilterRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentSummaryResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentUpdateRequest;
import pl.telco.incident.mapper.IncidentMapper;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;
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
    private final IncidentMapper incidentMapper;
    private final ObservabilityEventLogger observabilityEventLogger;

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
        observabilityEventLogger.logIncidentEvent("create", IncidentTimelineEventType.CREATED, saved, null, null, false);

        return incidentMapper.toDetailedResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getAllIncidents(IncidentFilterRequest filter) {
        validateSortBy(filter.getSortBy());

        Set<IncidentPriority> priorityFilters = mergePriorityFilters(filter.getPriority(), filter.getPriorities());
        Set<IncidentStatus> statusFilters = mergeStatusFilters(filter.getStatus(), filter.getStatuses());
        Sort.Direction sortDirection = parseSortDirection(filter.getDirection());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Specification<Incident> specification = Specification
                .where(hasPriorities(priorityFilters))
                .and(hasRegion(filter.getRegion()))
                .and(hasPossiblyPlanned(filter.getPossiblyPlanned()))
                .and(hasStatuses(statusFilters))
                .and(incidentNumberContains(filter.getIncidentNumber()))
                .and(titleContains(filter.getTitle()))
                .and(hasSourceAlarmType(filter.getSourceAlarmType()))
                .and(openedAtFrom(filter.getOpenedFrom()))
                .and(openedAtTo(filter.getOpenedTo()))
                .and(acknowledgedAtFrom(filter.getAcknowledgedFrom()))
                .and(acknowledgedAtTo(filter.getAcknowledgedTo()))
                .and(resolvedAtFrom(filter.getResolvedFrom()))
                .and(resolvedAtTo(filter.getResolvedTo()))
                .and(closedAtFrom(filter.getClosedFrom()))
                .and(closedAtTo(filter.getClosedTo()))
                .and(withSort(filter.getSortBy(), sortDirection));

        return incidentRepository.findAll(specification, pageable)
                .map(incidentMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return incidentMapper.toDetailedResponse(findDetailedIncidentByIdOrThrow(id));
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
        observabilityEventLogger.logIncidentEvent("update", IncidentTimelineEventType.UPDATED, saved, null, changedFields, false);

        return incidentMapper.toDetailedResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentTimelineResponse> getIncidentTimeline(Long id) {
        findIncidentByIdOrThrow(id);

        return incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(incidentMapper::toTimelineResponse)
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
        observabilityEventLogger.logIncidentEvent("acknowledge", IncidentTimelineEventType.ACKNOWLEDGED, saved, IncidentStatus.OPEN, null, hasNote(request));

        return incidentMapper.toDetailedResponse(saved);
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
        observabilityEventLogger.logIncidentEvent("resolve", IncidentTimelineEventType.RESOLVED, saved, IncidentStatus.ACKNOWLEDGED, null, hasNote(request));

        return incidentMapper.toDetailedResponse(saved);
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
        observabilityEventLogger.logIncidentEvent("close", IncidentTimelineEventType.CLOSED, saved, IncidentStatus.RESOLVED, null, hasNote(request));

        return incidentMapper.toDetailedResponse(saved);
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

}
