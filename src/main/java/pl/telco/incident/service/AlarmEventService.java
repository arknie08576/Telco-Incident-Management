package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private final JdbcTemplate jdbcTemplate;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;
    private final ObservabilityEventLogger observabilityEventLogger;

    @Transactional
    public AlarmEventResponse createAlarmEvent(AlarmEventCreateRequest request) {
        validateRequest(request);

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

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityType", "AlarmEvent");
        fields.put("tableName", "alarm_event");
        fields.put("entityId", alarmEventId);
        fields.put("externalId", request.getExternalId().trim());
        fields.put("networkNodeId", request.getNetworkNodeId());
        fields.put("incidentId", request.getIncidentId());
        fields.put("alarmType", request.getAlarmType().trim());
        fields.put("severity", request.getSeverity());
        fields.put("alarmStatus", request.getStatus());

        observabilityEventLogger.logEvent(
                "alarm",
                "persistence",
                "insert",
                "entity_change",
                fields
        );

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

    @Transactional(readOnly = true)
    public List<AlarmEventResponse> getAlarmEvents() {
        return jdbcTemplate.query(
                """
                SELECT id, source_system, external_id, network_node_id, incident_id,
                       alarm_type, severity, status, description,
                       suppressed_by_maintenance, occurred_at, received_at
                FROM alarm_event
                ORDER BY occurred_at DESC
                """,
                (rs, rowNum) -> {
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
        );
    }

    private void validateRequest(AlarmEventCreateRequest request) {
        if (!networkNodeRepository.existsById(request.getNetworkNodeId())) {
            throw new ResourceNotFoundException("Network node not found: " + request.getNetworkNodeId());
        }

        if (request.getIncidentId() != null && !incidentRepository.existsById(request.getIncidentId())) {
            throw new ResourceNotFoundException("Incident not found: " + request.getIncidentId());
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
}
