package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.dto.AlarmEventUpdateRequest;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "ae.id",
            "externalId", "ae.external_id",
            "sourceSystem", "ae.source_system",
            "alarmType", "ae.alarm_type",
            "severity", "ae.severity",
            "status", "ae.status",
            "occurredAt", "ae.occurred_at",
            "receivedAt", "ae.received_at"
    );

    private final JdbcTemplate jdbcTemplate;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;
    private final ObservabilityEventLogger observabilityEventLogger;

    @Transactional
    public AlarmEventResponse createAlarmEvent(AlarmEventCreateRequest request) {
        validateCreateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Long alarmEventId = jdbcTemplate.queryForObject(
                """
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                request.getSourceSystem().trim(),
                request.getExternalId().trim(),
                request.getNetworkNodeId(),
                request.getIncidentId(),
                request.getAlarmType().trim(),
                request.getSeverity().name(),
                request.getStatus().name(),
                request.getDescription(),
                Boolean.TRUE.equals(request.getSuppressedByMaintenance()),
                Timestamp.valueOf(request.getOccurredAt()),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );

        Map<String, Object> createFields = new LinkedHashMap<>();
        createFields.put("externalId", request.getExternalId().trim());
        createFields.put("networkNodeId", request.getNetworkNodeId());
        createFields.put("incidentId", request.getIncidentId());
        createFields.put("alarmType", request.getAlarmType().trim());
        createFields.put("severity", request.getSeverity());
        createFields.put("alarmStatus", request.getStatus());
        logPersistenceEvent(alarmEventId, "insert", createFields);

        AlarmEventResponse response = new AlarmEventResponse();
        response.setId(alarmEventId);
        response.setSourceSystem(request.getSourceSystem().trim());
        response.setExternalId(request.getExternalId().trim());
        response.setNetworkNodeId(request.getNetworkNodeId());
        response.setIncidentId(request.getIncidentId());
        response.setAlarmType(request.getAlarmType().trim());
        response.setSeverity(request.getSeverity());
        response.setStatus(request.getStatus());
        response.setDescription(request.getDescription());
        response.setSuppressedByMaintenance(Boolean.TRUE.equals(request.getSuppressedByMaintenance()));
        response.setOccurredAt(request.getOccurredAt());
        response.setReceivedAt(now);
        return response;
    }

    @Transactional
    public AlarmEventResponse updateAlarmEvent(Long id, AlarmEventUpdateRequest request) {
        AlarmEventResponse current = getAlarmEventOrThrow(id);

        Long incidentId = current.getIncidentId();
        String alarmType = current.getAlarmType();
        AlarmSeverity severity = current.getSeverity();
        AlarmStatus status = current.getStatus();
        String description = current.getDescription();
        Boolean suppressedByMaintenance = current.getSuppressedByMaintenance();
        LocalDateTime occurredAt = current.getOccurredAt();
        List<String> changedFields = new ArrayList<>();

        if (request.getIncidentId() != null && !Objects.equals(request.getIncidentId(), incidentId)) {
            validateIncidentExists(request.getIncidentId());
            incidentId = request.getIncidentId();
            changedFields.add("incidentId");
        }

        if (request.getAlarmType() != null) {
            String normalizedAlarmType = request.getAlarmType().trim();
            if (!Objects.equals(normalizedAlarmType, alarmType)) {
                alarmType = normalizedAlarmType;
                changedFields.add("alarmType");
            }
        }

        if (request.getSeverity() != null && request.getSeverity() != severity) {
            severity = request.getSeverity();
            changedFields.add("severity");
        }

        if (request.getStatus() != null && request.getStatus() != status) {
            status = request.getStatus();
            changedFields.add("status");
        }

        if (request.getDescription() != null && !Objects.equals(request.getDescription(), description)) {
            description = request.getDescription();
            changedFields.add("description");
        }

        if (request.getSuppressedByMaintenance() != null
                && !Objects.equals(request.getSuppressedByMaintenance(), suppressedByMaintenance)) {
            suppressedByMaintenance = request.getSuppressedByMaintenance();
            changedFields.add("suppressedByMaintenance");
        }

        if (request.getOccurredAt() != null && !Objects.equals(request.getOccurredAt(), occurredAt)) {
            occurredAt = request.getOccurredAt();
            changedFields.add("occurredAt");
        }

        if (changedFields.isEmpty()) {
            throw new BadRequestException("Patch request does not change alarm event");
        }

        jdbcTemplate.update(
                """
                UPDATE alarm_event
                SET incident_id = ?, alarm_type = ?, severity = ?, status = ?, description = ?,
                    suppressed_by_maintenance = ?, occurred_at = ?
                WHERE id = ?
                """,
                incidentId,
                alarmType,
                severity.name(),
                status.name(),
                description,
                Boolean.TRUE.equals(suppressedByMaintenance),
                Timestamp.valueOf(occurredAt),
                id
        );

        Map<String, Object> updateFields = new LinkedHashMap<>();
        updateFields.put("externalId", current.getExternalId());
        updateFields.put("networkNodeId", current.getNetworkNodeId());
        updateFields.put("incidentId", incidentId);
        updateFields.put("alarmType", alarmType);
        updateFields.put("severity", severity);
        updateFields.put("alarmStatus", status);
        updateFields.put("changedFields", changedFields);
        logPersistenceEvent(id, "update", updateFields);

        return getAlarmEventOrThrow(id);
    }

    @Transactional(readOnly = true)
    public AlarmEventResponse getAlarmEventById(Long id) {
        return getAlarmEventOrThrow(id);
    }

    @Transactional(readOnly = true)
    public Page<AlarmEventResponse> getAlarmEvents(
            int page,
            int size,
            String sortBy,
            String direction,
            AlarmSeverity severity,
            List<String> severities,
            AlarmStatus status,
            List<String> statuses,
            String sourceSystem,
            String externalId,
            String alarmType,
            Long networkNodeId,
            Long incidentId,
            Boolean suppressedByMaintenance,
            LocalDateTime occurredFrom,
            LocalDateTime occurredTo,
            LocalDateTime receivedFrom,
            LocalDateTime receivedTo
    ) {
        validateSortBy(sortBy);
        validateDateRange("occurredFrom", occurredFrom, "occurredTo", occurredTo);
        validateDateRange("receivedFrom", receivedFrom, "receivedTo", receivedTo);

        Set<AlarmSeverity> severityFilters = mergeSeverityFilters(severity, severities);
        Set<AlarmStatus> statusFilters = mergeStatusFilters(status, statuses);
        Sort.Direction sortDirection = parseSortDirection(direction);
        Pageable pageable = PageRequest.of(page, size);

        QueryFragments queryFragments = buildWhereClause(
                severityFilters,
                statusFilters,
                sourceSystem,
                externalId,
                alarmType,
                networkNodeId,
                incidentId,
                suppressedByMaintenance,
                occurredFrom,
                occurredTo,
                receivedFrom,
                receivedTo
        );

        String listSql = """
                SELECT ae.id, ae.source_system, ae.external_id, ae.network_node_id, ae.incident_id,
                       ae.alarm_type, ae.severity, ae.status, ae.description,
                       ae.suppressed_by_maintenance, ae.occurred_at, ae.received_at
                FROM alarm_event ae
                """ + queryFragments.whereClause()
                + " ORDER BY " + ALLOWED_SORT_FIELDS.get(sortBy) + " " + sortDirection.name() + ", ae.id DESC"
                + " LIMIT ? OFFSET ?";

        List<Object> listParams = new ArrayList<>(queryFragments.params());
        listParams.add(size);
        listParams.add((long) page * size);

        List<AlarmEventResponse> content = jdbcTemplate.query(
                listSql,
                (rs, rowNum) -> mapAlarmEventRow(rs),
                listParams.toArray()
        );

        Long totalElements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alarm_event ae " + queryFragments.whereClause(),
                Long.class,
                queryFragments.params().toArray()
        );

        return new PageImpl<>(content, pageable, totalElements == null ? 0 : totalElements);
    }

    private AlarmEventResponse getAlarmEventOrThrow(Long id) {
        AlarmEventResponse response = jdbcTemplate.query(
                """
                SELECT id, source_system, external_id, network_node_id, incident_id,
                       alarm_type, severity, status, description,
                       suppressed_by_maintenance, occurred_at, received_at
                FROM alarm_event
                WHERE id = ?
                """,
                (ResultSetExtractor<AlarmEventResponse>) rs -> rs.next() ? mapAlarmEventRow(rs) : null,
                id
        );

        if (response == null) {
            throw new ResourceNotFoundException("Alarm event not found: " + id);
        }

        return response;
    }

    private AlarmEventResponse mapAlarmEventRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        AlarmEventResponse response = new AlarmEventResponse();
        response.setId(rs.getLong("id"));
        response.setSourceSystem(rs.getString("source_system"));
        response.setExternalId(rs.getString("external_id"));
        response.setNetworkNodeId(rs.getLong("network_node_id"));
        Object incidentId = rs.getObject("incident_id");
        response.setIncidentId(incidentId == null ? null : ((Number) incidentId).longValue());
        response.setAlarmType(rs.getString("alarm_type"));
        response.setSeverity(AlarmSeverity.valueOf(rs.getString("severity")));
        response.setStatus(AlarmStatus.valueOf(rs.getString("status")));
        response.setDescription(rs.getString("description"));
        response.setSuppressedByMaintenance(rs.getBoolean("suppressed_by_maintenance"));
        response.setOccurredAt(rs.getTimestamp("occurred_at").toLocalDateTime());
        response.setReceivedAt(rs.getTimestamp("received_at").toLocalDateTime());
        return response;
    }

    private QueryFragments buildWhereClause(
            Set<AlarmSeverity> severityFilters,
            Set<AlarmStatus> statusFilters,
            String sourceSystem,
            String externalId,
            String alarmType,
            Long networkNodeId,
            Long incidentId,
            Boolean suppressedByMaintenance,
            LocalDateTime occurredFrom,
            LocalDateTime occurredTo,
            LocalDateTime receivedFrom,
            LocalDateTime receivedTo
    ) {
        StringBuilder whereClause = new StringBuilder("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (!severityFilters.isEmpty()) {
            whereClause.append(" AND ae.severity IN (")
                    .append(buildPlaceholders(severityFilters.size()))
                    .append(")");
            severityFilters.forEach(value -> params.add(value.name()));
        }

        if (!statusFilters.isEmpty()) {
            whereClause.append(" AND ae.status IN (")
                    .append(buildPlaceholders(statusFilters.size()))
                    .append(")");
            statusFilters.forEach(value -> params.add(value.name()));
        }

        applyTextFilter(whereClause, params, "ae.source_system", sourceSystem);
        applyTextFilter(whereClause, params, "ae.external_id", externalId);
        applyTextFilter(whereClause, params, "ae.alarm_type", alarmType);

        if (networkNodeId != null) {
            whereClause.append(" AND ae.network_node_id = ?");
            params.add(networkNodeId);
        }

        if (incidentId != null) {
            whereClause.append(" AND ae.incident_id = ?");
            params.add(incidentId);
        }

        if (suppressedByMaintenance != null) {
            whereClause.append(" AND ae.suppressed_by_maintenance = ?");
            params.add(suppressedByMaintenance);
        }

        if (occurredFrom != null) {
            whereClause.append(" AND ae.occurred_at >= ?");
            params.add(Timestamp.valueOf(occurredFrom));
        }

        if (occurredTo != null) {
            whereClause.append(" AND ae.occurred_at <= ?");
            params.add(Timestamp.valueOf(occurredTo));
        }

        if (receivedFrom != null) {
            whereClause.append(" AND ae.received_at >= ?");
            params.add(Timestamp.valueOf(receivedFrom));
        }

        if (receivedTo != null) {
            whereClause.append(" AND ae.received_at <= ?");
            params.add(Timestamp.valueOf(receivedTo));
        }

        return new QueryFragments(whereClause.toString(), params);
    }

    private void applyTextFilter(StringBuilder whereClause, List<Object> params, String column, String value) {
        if (value != null && !value.isBlank()) {
            whereClause.append(" AND LOWER(").append(column).append(") LIKE ?");
            params.add("%" + value.trim().toLowerCase(Locale.ROOT) + "%");
        }
    }

    private void validateCreateRequest(AlarmEventCreateRequest request) {
        validateNetworkNodeExists(request.getNetworkNodeId());

        if (request.getIncidentId() != null) {
            validateIncidentExists(request.getIncidentId());
        }

        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alarm_event WHERE source_system = ? AND external_id = ?",
                Integer.class,
                request.getSourceSystem().trim(),
                request.getExternalId().trim()
        );

        if (existingCount != null && existingCount > 0) {
            throw new ConflictException("Alarm event with sourceSystem and externalId already exists");
        }
    }

    private void validateNetworkNodeExists(Long networkNodeId) {
        if (!networkNodeRepository.existsById(networkNodeId)) {
            throw new ResourceNotFoundException("Network node not found: " + networkNodeId);
        }
    }

    private void validateIncidentExists(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found: " + incidentId);
        }
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

    private String buildPlaceholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private void logPersistenceEvent(Long entityId, String action, Map<String, Object> additionalFields) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityType", "AlarmEvent");
        fields.put("tableName", "alarm_event");
        fields.put("entityId", entityId);
        fields.putAll(additionalFields);

        observabilityEventLogger.logEvent(
                "alarm",
                "persistence",
                action,
                "entity_change",
                fields
        );
    }

    private record QueryFragments(String whereClause, List<Object> params) {
    }
}
