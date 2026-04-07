package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventFilterRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.dto.AlarmEventUpdateRequest;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static pl.telco.incident.repository.specification.AlarmEventSpecifications.alarmTypeContains;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.externalIdContains;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.hasIncidentId;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.hasNetworkNodeId;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.hasSeverities;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.hasStatuses;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.hasSuppressedByMaintenance;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.occurredAtFrom;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.occurredAtTo;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.receivedAtFrom;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.receivedAtTo;
import static pl.telco.incident.repository.specification.AlarmEventSpecifications.sourceSystemContains;

@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "externalId", "externalId",
            "sourceSystem", "sourceSystem",
            "alarmType", "alarmType",
            "severity", "severity",
            "status", "status",
            "occurredAt", "occurredAt",
            "receivedAt", "receivedAt"
    );

    private final AlarmEventRepository alarmEventRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public AlarmEventResponse createAlarmEvent(AlarmEventCreateRequest request) {
        validateCreateRequest(request);

        AlarmEvent alarmEvent = new AlarmEvent();
        alarmEvent.setSourceSystem(request.getSourceSystem().trim());
        alarmEvent.setExternalId(request.getExternalId().trim());
        alarmEvent.setNetworkNode(getNetworkNodeOrThrow(request.getNetworkNodeId()));
        alarmEvent.setIncident(request.getIncidentId() == null ? null : getIncidentOrThrow(request.getIncidentId()));
        alarmEvent.setAlarmType(request.getAlarmType().trim());
        alarmEvent.setSeverity(request.getSeverity());
        alarmEvent.setStatus(request.getStatus());
        alarmEvent.setDescription(request.getDescription());
        alarmEvent.setSuppressedByMaintenance(Boolean.TRUE.equals(request.getSuppressedByMaintenance()));
        alarmEvent.setOccurredAt(request.getOccurredAt());

        return mapToResponse(alarmEventRepository.save(alarmEvent));
    }

    @Transactional
    public AlarmEventResponse updateAlarmEvent(Long id, AlarmEventUpdateRequest request) {
        AlarmEvent alarmEvent = getAlarmEventEntityOrThrow(id);

        Long currentIncidentId = alarmEvent.getIncident() != null ? alarmEvent.getIncident().getId() : null;
        String alarmType = alarmEvent.getAlarmType();
        AlarmSeverity severity = alarmEvent.getSeverity();
        AlarmStatus status = alarmEvent.getStatus();
        String description = alarmEvent.getDescription();
        Boolean suppressedByMaintenance = alarmEvent.getSuppressedByMaintenance();
        LocalDateTime occurredAt = alarmEvent.getOccurredAt();
        boolean changed = false;

        if (request.getIncidentId() != null && !Objects.equals(request.getIncidentId(), currentIncidentId)) {
            alarmEvent.setIncident(getIncidentOrThrow(request.getIncidentId()));
            changed = true;
        }

        if (request.getAlarmType() != null) {
            String normalizedAlarmType = request.getAlarmType().trim();
            if (!Objects.equals(normalizedAlarmType, alarmType)) {
                alarmEvent.setAlarmType(normalizedAlarmType);
                changed = true;
            }
        }

        if (request.getSeverity() != null && request.getSeverity() != severity) {
            alarmEvent.setSeverity(request.getSeverity());
            changed = true;
        }

        if (request.getStatus() != null && request.getStatus() != status) {
            alarmEvent.setStatus(request.getStatus());
            changed = true;
        }

        if (request.getDescription() != null && !Objects.equals(request.getDescription(), description)) {
            alarmEvent.setDescription(request.getDescription());
            changed = true;
        }

        if (request.getSuppressedByMaintenance() != null
                && !Objects.equals(request.getSuppressedByMaintenance(), suppressedByMaintenance)) {
            alarmEvent.setSuppressedByMaintenance(request.getSuppressedByMaintenance());
            changed = true;
        }

        if (request.getOccurredAt() != null && !Objects.equals(request.getOccurredAt(), occurredAt)) {
            alarmEvent.setOccurredAt(request.getOccurredAt());
            changed = true;
        }

        if (!changed) {
            throw new BadRequestException("Patch request does not change alarm event");
        }

        return mapToResponse(alarmEventRepository.save(alarmEvent));
    }

    @Transactional(readOnly = true)
    public AlarmEventResponse getAlarmEventById(Long id) {
        return mapToResponse(getAlarmEventEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AlarmEventResponse> getAlarmEvents(AlarmEventFilterRequest filter) {
        validateSortBy(filter.getSortBy());
        validateDateRange("occurredFrom", filter.getOccurredFrom(), "occurredTo", filter.getOccurredTo());
        validateDateRange("receivedFrom", filter.getReceivedFrom(), "receivedTo", filter.getReceivedTo());

        Set<AlarmSeverity> severityFilters = mergeSeverityFilters(filter.getSeverity(), filter.getSeverities());
        Set<AlarmStatus> statusFilters = mergeStatusFilters(filter.getStatus(), filter.getStatuses());
        Sort.Direction sortDirection = parseSortDirection(filter.getDirection());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), buildSort(filter.getSortBy(), sortDirection));

        Specification<AlarmEvent> specification = Specification
                .where(hasSeverities(severityFilters))
                .and(hasStatuses(statusFilters))
                .and(sourceSystemContains(filter.getSourceSystem()))
                .and(externalIdContains(filter.getExternalId()))
                .and(alarmTypeContains(filter.getAlarmType()))
                .and(hasNetworkNodeId(filter.getNetworkNodeId()))
                .and(hasIncidentId(filter.getIncidentId()))
                .and(hasSuppressedByMaintenance(filter.getSuppressedByMaintenance()))
                .and(occurredAtFrom(filter.getOccurredFrom()))
                .and(occurredAtTo(filter.getOccurredTo()))
                .and(receivedAtFrom(filter.getReceivedFrom()))
                .and(receivedAtTo(filter.getReceivedTo()));

        return alarmEventRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    private AlarmEvent getAlarmEventEntityOrThrow(Long id) {
        return alarmEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm event not found: " + id));
    }

    private AlarmEventResponse mapToResponse(AlarmEvent alarmEvent) {
        AlarmEventResponse response = new AlarmEventResponse();
        response.setId(alarmEvent.getId());
        response.setSourceSystem(alarmEvent.getSourceSystem());
        response.setExternalId(alarmEvent.getExternalId());
        response.setNetworkNodeId(alarmEvent.getNetworkNode().getId());
        response.setIncidentId(alarmEvent.getIncident() != null ? alarmEvent.getIncident().getId() : null);
        response.setAlarmType(alarmEvent.getAlarmType());
        response.setSeverity(alarmEvent.getSeverity());
        response.setStatus(alarmEvent.getStatus());
        response.setDescription(alarmEvent.getDescription());
        response.setSuppressedByMaintenance(alarmEvent.getSuppressedByMaintenance());
        response.setOccurredAt(alarmEvent.getOccurredAt());
        response.setReceivedAt(alarmEvent.getReceivedAt());
        return response;
    }

    private void validateCreateRequest(AlarmEventCreateRequest request) {
        getNetworkNodeOrThrow(request.getNetworkNodeId());

        if (request.getIncidentId() != null) {
            getIncidentOrThrow(request.getIncidentId());
        }

        if (alarmEventRepository.existsBySourceSystemAndExternalId(request.getSourceSystem().trim(), request.getExternalId().trim())) {
            throw new ConflictException("Alarm event with sourceSystem and externalId already exists");
        }
    }

    private NetworkNode getNetworkNodeOrThrow(Long networkNodeId) {
        return networkNodeRepository.findById(networkNodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + networkNodeId));
    }

    private Incident getIncidentOrThrow(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
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

    private void validateDateRange(String fromFieldName, LocalDateTime from, String toFieldName, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(fromFieldName + " must be earlier than or equal to " + toFieldName);
        }
    }

    private Set<AlarmSeverity> mergeSeverityFilters(AlarmSeverity severity, List<String> severities) {
        LinkedHashSet<AlarmSeverity> merged = new LinkedHashSet<>();

        if (severity != null) {
            merged.add(severity);
        }
        merged.addAll(parseEnumFilters(severities, AlarmSeverity.class, "severities"));

        return merged;
    }

    private Set<AlarmStatus> mergeStatusFilters(AlarmStatus status, List<String> statuses) {
        LinkedHashSet<AlarmStatus> merged = new LinkedHashSet<>();

        if (status != null) {
            merged.add(status);
        }
        merged.addAll(parseEnumFilters(statuses, AlarmStatus.class, "statuses"));

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
