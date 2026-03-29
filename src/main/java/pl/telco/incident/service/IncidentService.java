package pl.telco.incident.service;

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
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
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
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static pl.telco.incident.repository.specification.IncidentSpecifications.hasPossiblyPlanned;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasPriority;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasRegion;
import static pl.telco.incident.repository.specification.IncidentSpecifications.hasStatus;
import static pl.telco.incident.repository.specification.IncidentSpecifications.openedAtFrom;
import static pl.telco.incident.repository.specification.IncidentSpecifications.openedAtTo;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "openedAt",
            "incidentNumber",
            "priority",
            "title"
    );

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request) {
        validateIncidentNumberUniqueness(request.getIncidentNumber());
        validateNodeUniqueness(request);
        validateRootNodeConsistency(request);

        NetworkNode rootNode = networkNodeRepository.findById(request.getRootNodeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Root node not found: " + request.getRootNodeId()
                ));

        Incident incident = new Incident();
        incident.setIncidentNumber(request.getIncidentNumber());
        incident.setTitle(request.getTitle());
        incident.setPriority(request.getPriority());
        incident.setRegion(request.getRegion());
        incident.setSourceAlarmType(request.getSourceAlarmType());
        incident.setPossiblyPlanned(request.getPossiblyPlanned());
        incident.setRootNode(rootNode);

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            NetworkNode networkNode = networkNodeRepository.findById(nodeRequest.getNetworkNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Network node not found: " + nodeRequest.getNetworkNodeId()
                    ));

            IncidentNode incidentNode = new IncidentNode();
            incidentNode.setNetworkNode(networkNode);
            incidentNode.setRole(nodeRequest.getRole());

            incident.addIncidentNode(incidentNode);
        }

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(
                saved,
                "CREATED",
                "Incident created"
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getAllIncidents(
            int page,
            int size,
            String sortBy,
            String direction,
            IncidentPriority priority,
            String region,
            Boolean possiblyPlanned,
            IncidentStatus status,
            LocalDateTime openedFrom,
            LocalDateTime openedTo
    ) {
        validateSortBy(sortBy);
        validateOpenedAtRange(openedFrom, openedTo);

        Sort.Direction sortDirection = parseSortDirection(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Specification<Incident> specification = Specification
                .where(hasPriority(priority))
                .and(hasRegion(region))
                .and(hasPossiblyPlanned(possiblyPlanned))
                .and(hasStatus(status))
                .and(openedAtFrom(openedFrom))
                .and(openedAtTo(openedTo));

        return incidentRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        Incident incident = findIncidentByIdOrThrow(id);
        return mapToResponse(incident);
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

        validateStatusTransition(
                incident,
                IncidentStatus.OPEN,
                "Only OPEN incidents can be acknowledged"
        );

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(
                saved,
                "ACKNOWLEDGED",
                buildLifecycleMessage("Incident acknowledged", request)
        );

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(
                incident,
                IncidentStatus.ACKNOWLEDGED,
                "Only ACKNOWLEDGED incidents can be resolved"
        );

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(
                saved,
                "RESOLVED",
                buildLifecycleMessage("Incident resolved", request)
        );

        return mapToResponse(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, IncidentActionRequest request) {
        Incident incident = findIncidentByIdOrThrow(id);

        validateStatusTransition(
                incident,
                IncidentStatus.RESOLVED,
                "Only RESOLVED incidents can be closed"
        );

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(LocalDateTime.now());

        Incident saved = incidentRepository.save(incident);

        addTimelineEvent(
                saved,
                "CLOSED",
                buildLifecycleMessage("Incident closed", request)
        );

        return mapToResponse(saved);
    }

    private Incident findIncidentByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incident not found: " + id
                ));
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
        if (request == null || request.getNote() == null || request.getNote().isBlank()) {
            return defaultMessage;
        }

        return defaultMessage + ": " + request.getNote().trim();
    }

    private void validateIncidentNumberUniqueness(String incidentNumber) {
        if (incidentRepository.findByIncidentNumber(incidentNumber).isPresent()) {
            throw new ConflictException("Incident with number already exists: " + incidentNumber);
        }
    }

    private void validateNodeUniqueness(IncidentCreateRequest request) {
        Set<Long> uniqueNodeIds = new HashSet<>();

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            if (!uniqueNodeIds.add(nodeRequest.getNetworkNodeId())) {
                throw new BadRequestException(
                        "Duplicate networkNodeId in nodes: " + nodeRequest.getNetworkNodeId()
                );
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

    private void validateRootNodeConsistency(IncidentCreateRequest request) {
        long rootCount = request.getNodes().stream()
                .filter(node -> node.getRole() == IncidentNodeRole.ROOT)
                .count();

        if (rootCount != 1) {
            throw new BadRequestException("Exactly one node must have role ROOT");
        }

        boolean rootMatches = request.getNodes().stream()
                .anyMatch(node ->
                        node.getRole() == IncidentNodeRole.ROOT
                                && request.getRootNodeId().equals(node.getNetworkNodeId())
                );

        if (!rootMatches) {
            throw new BadRequestException("rootNodeId must match the node with role ROOT");
        }
    }

    private void validateOpenedAtRange(LocalDateTime openedFrom, LocalDateTime openedTo) {
        if (openedFrom != null && openedTo != null && openedFrom.isAfter(openedTo)) {
            throw new BadRequestException("openedFrom must be earlier than or equal to openedTo");
        }
    }

    private IncidentResponse mapToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
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

    private IncidentTimelineResponse mapTimelineToResponse(IncidentTimeline timeline) {
        IncidentTimelineResponse response = new IncidentTimelineResponse();
        response.setId(timeline.getId());
        response.setEventType(timeline.getEventType());
        response.setMessage(timeline.getMessage());
        response.setCreatedAt(timeline.getCreatedAt());
        return response;
    }
}
