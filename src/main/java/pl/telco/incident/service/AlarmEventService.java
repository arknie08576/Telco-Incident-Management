package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.AlarmEventRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentNodeRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private final AlarmEventRepository alarmEventRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentNodeRepository incidentNodeRepository;

    @Transactional(readOnly = true)
    public List<AlarmEventResponse> getAllAlarmEvents() {
        return alarmEventRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlarmEventResponse getAlarmEventById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public AlarmEventResponse createAlarmEvent(AlarmEventRequest request) {
        validateUniqueness(request.getSourceSystem(), request.getExternalId());

        AlarmEvent alarmEvent = new AlarmEvent();
        applyRequest(alarmEvent, request);

        AlarmEvent saved = alarmEventRepository.save(alarmEvent);
        logAlarmEventCrudEvent("create", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public AlarmEventResponse updateAlarmEvent(Long id, AlarmEventRequest request) {
        AlarmEvent alarmEvent = findByIdOrThrow(id);
        String sourceSystem = request.getSourceSystem().trim();
        String externalId = request.getExternalId().trim();

        if (alarmEventRepository.existsBySourceSystemAndExternalIdAndIdNot(sourceSystem, externalId, id)) {
            throw new ConflictException(
                    "Alarm event with sourceSystem/externalId already exists: " + sourceSystem + "/" + externalId
            );
        }

        applyRequest(alarmEvent, request);

        AlarmEvent saved = alarmEventRepository.save(alarmEvent);
        logAlarmEventCrudEvent("update", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAlarmEvent(Long id) {
        AlarmEvent alarmEvent = findByIdOrThrow(id);
        logAlarmEventCrudEvent("delete", alarmEvent);
        alarmEventRepository.delete(alarmEvent);
    }

    private AlarmEvent findByIdOrThrow(Long id) {
        return alarmEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm event not found: " + id));
    }

    private void validateUniqueness(String sourceSystem, String externalId) {
        String normalizedSourceSystem = sourceSystem.trim();
        String normalizedExternalId = externalId.trim();

        if (alarmEventRepository.existsBySourceSystemAndExternalId(normalizedSourceSystem, normalizedExternalId)) {
            throw new ConflictException(
                    "Alarm event with sourceSystem/externalId already exists: "
                            + normalizedSourceSystem + "/" + normalizedExternalId
            );
        }
    }

    private void applyRequest(AlarmEvent alarmEvent, AlarmEventRequest request) {
        NetworkNode networkNode = networkNodeRepository.findById(request.getNetworkNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + request.getNetworkNodeId()));

        Incident incident = null;
        if (request.getIncidentId() != null) {
            incident = incidentRepository.findById(request.getIncidentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + request.getIncidentId()));
        }

        validateRequest(request, networkNode, incident);

        alarmEvent.setSourceSystem(request.getSourceSystem().trim());
        alarmEvent.setExternalId(request.getExternalId().trim());
        alarmEvent.setNetworkNode(networkNode);
        alarmEvent.setIncident(incident);
        alarmEvent.setAlarmType(request.getAlarmType().trim());
        alarmEvent.setSeverity(request.getSeverity());
        alarmEvent.setStatus(request.getStatus());
        alarmEvent.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        alarmEvent.setSuppressedByMaintenance(request.getSuppressedByMaintenance());
        alarmEvent.setOccurredAt(request.getOccurredAt());
        alarmEvent.setReceivedAt(request.getReceivedAt());
    }

    private void validateRequest(AlarmEventRequest request, NetworkNode networkNode, Incident incident) {
        if (request.getReceivedAt() != null && request.getReceivedAt().isBefore(request.getOccurredAt())) {
            throw new BadRequestException("receivedAt must be greater than or equal to occurredAt");
        }

        if (incident != null && !incidentNodeRepository.existsByIncidentIdAndNetworkNodeId(incident.getId(), networkNode.getId())) {
            throw new BadRequestException(
                    "Referenced incident does not include network node: %d/%d"
                            .formatted(incident.getId(), networkNode.getId())
            );
        }
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
        response.setCreatedAt(alarmEvent.getCreatedAt());
        return response;
    }

    private void logAlarmEventCrudEvent(String eventAction, AlarmEvent alarmEvent) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityId", alarmEvent.getId());
        fields.put("sourceSystem", alarmEvent.getSourceSystem());
        fields.put("externalId", alarmEvent.getExternalId());
        fields.put("networkNodeId", alarmEvent.getNetworkNode().getId());
        fields.put("incidentId", alarmEvent.getIncident() != null ? alarmEvent.getIncident().getId() : null);
        fields.put("alarmType", alarmEvent.getAlarmType());
        fields.put("severity", alarmEvent.getSeverity());
        fields.put("alarmEventStatus", alarmEvent.getStatus());
        fields.put("suppressedByMaintenance", alarmEvent.getSuppressedByMaintenance());
        fields.put("occurredAt", alarmEvent.getOccurredAt());
        fields.put("receivedAt", alarmEvent.getReceivedAt());
        fields.put("createdAt", alarmEvent.getCreatedAt());

        CrudEventLogger.log(log, "alarm_event", eventAction, fields);
    }
}
