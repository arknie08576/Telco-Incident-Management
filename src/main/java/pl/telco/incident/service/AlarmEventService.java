package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.AlarmEventRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmEventService {

    private final AlarmEventRepository alarmEventRepository;
    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;

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

        return mapToResponse(alarmEventRepository.save(alarmEvent));
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

        return mapToResponse(alarmEventRepository.save(alarmEvent));
    }

    @Transactional
    public void deleteAlarmEvent(Long id) {
        AlarmEvent alarmEvent = findByIdOrThrow(id);
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

        alarmEvent.setSourceSystem(request.getSourceSystem().trim());
        alarmEvent.setExternalId(request.getExternalId().trim());
        alarmEvent.setNetworkNode(networkNode);
        alarmEvent.setIncident(incident);
        alarmEvent.setAlarmType(request.getAlarmType().trim());
        alarmEvent.setSeverity(request.getSeverity());
        alarmEvent.setStatus(request.getStatus());
        alarmEvent.setDescription(request.getDescription());
        alarmEvent.setSuppressedByMaintenance(request.getSuppressedByMaintenance());
        alarmEvent.setOccurredAt(request.getOccurredAt());
        alarmEvent.setReceivedAt(request.getReceivedAt());
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
}
