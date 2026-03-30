package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.IncidentTimelineEntryRequest;
import pl.telco.incident.dto.IncidentTimelineEntryResponse;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentTimelineEntryService {

    private final IncidentTimelineRepository incidentTimelineRepository;
    private final IncidentRepository incidentRepository;

    @Transactional(readOnly = true)
    public List<IncidentTimelineEntryResponse> getAllIncidentTimelineEntries() {
        return incidentTimelineRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentTimelineEntryResponse getIncidentTimelineEntryById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public IncidentTimelineEntryResponse createIncidentTimelineEntry(IncidentTimelineEntryRequest request) {
        IncidentTimeline timeline = new IncidentTimeline();
        applyRequest(timeline, request);
        IncidentTimeline saved = incidentTimelineRepository.save(timeline);
        logIncidentTimelineCrudEvent("create", saved, "direct");
        return mapToResponse(saved);
    }

    @Transactional
    public IncidentTimelineEntryResponse updateIncidentTimelineEntry(Long id, IncidentTimelineEntryRequest request) {
        IncidentTimeline timeline = findByIdOrThrow(id);
        applyRequest(timeline, request);
        IncidentTimeline saved = incidentTimelineRepository.save(timeline);
        logIncidentTimelineCrudEvent("update", saved, "direct");
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteIncidentTimelineEntry(Long id) {
        IncidentTimeline timeline = findByIdOrThrow(id);
        logIncidentTimelineCrudEvent("delete", timeline, "direct");
        incidentTimelineRepository.delete(timeline);
    }

    private IncidentTimeline findByIdOrThrow(Long id) {
        return incidentTimelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident timeline entry not found: " + id));
    }

    private Incident findIncidentByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private void applyRequest(IncidentTimeline timeline, IncidentTimelineEntryRequest request) {
        timeline.setIncident(findIncidentByIdOrThrow(request.getIncidentId()));
        timeline.setEventType(request.getEventType().trim());
        timeline.setMessage(request.getMessage().trim());
    }

    private IncidentTimelineEntryResponse mapToResponse(IncidentTimeline timeline) {
        IncidentTimelineEntryResponse response = new IncidentTimelineEntryResponse();
        response.setId(timeline.getId());
        response.setIncidentId(timeline.getIncident().getId());
        response.setEventType(timeline.getEventType());
        response.setMessage(timeline.getMessage());
        response.setCreatedAt(timeline.getCreatedAt());
        return response;
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
}
