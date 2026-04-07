package pl.telco.incident.observability;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ObservabilityEventLogger {

    public void logEvent(
            String dataset,
            String category,
            String action,
            String message,
            Map<String, Object> additionalFields
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("eventDataset", dataset);
        fields.put("eventCategory", category);
        fields.put("eventAction", action);

        if (additionalFields != null && !additionalFields.isEmpty()) {
            fields.putAll(additionalFields);
        }

        log.info("{} {}", message, StructuredArguments.entries(fields));
    }

    public void logIncidentEvent(
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
}
