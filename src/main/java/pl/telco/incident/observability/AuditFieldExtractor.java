package pl.telco.incident.observability;

import org.springframework.stereotype.Component;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuditFieldExtractor {

    public Map<String, Object> extract(Object entity, String operation) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("persistenceOperation", operation);

        if (entity instanceof Incident incident) {
            fields.put("eventDataset", "incident");
            fields.put("entityType", "Incident");
            fields.put("tableName", "incident");
            fields.put("entityId", incident.getId());
            fields.put("incidentNumber", incident.getIncidentNumber());
            fields.put("incidentStatus", incident.getStatus());
            fields.put("priority", incident.getPriority());
            fields.put("region", incident.getRegion());
            fields.put("sourceAlarmType", incident.getSourceAlarmType());
            fields.put("rootNodeId", incident.getRootNode() != null ? incident.getRootNode().getId() : null);
            fields.put("possiblyPlanned", incident.getPossiblyPlanned());
            return fields;
        }

        if (entity instanceof IncidentNode incidentNode) {
            fields.put("eventDataset", "incident");
            fields.put("entityType", "IncidentNode");
            fields.put("tableName", "incident_node");
            fields.put("entityId", incidentNode.getId());
            fields.put("incidentId", incidentNode.getIncident() != null ? incidentNode.getIncident().getId() : null);
            fields.put("networkNodeId", incidentNode.getNetworkNode() != null ? incidentNode.getNetworkNode().getId() : null);
            fields.put("nodeRole", incidentNode.getRole());
            return fields;
        }

        if (entity instanceof IncidentTimeline timeline) {
            fields.put("eventDataset", "incident");
            fields.put("entityType", "IncidentTimeline");
            fields.put("tableName", "incident_timeline");
            fields.put("entityId", timeline.getId());
            fields.put("incidentId", timeline.getIncident() != null ? timeline.getIncident().getId() : null);
            fields.put("timelineEventType", timeline.getEventType());
            return fields;
        }

        if (entity instanceof NetworkNode networkNode) {
            fields.put("eventDataset", "network_node");
            fields.put("entityType", "NetworkNode");
            fields.put("tableName", "network_node");
            fields.put("entityId", networkNode.getId());
            fields.put("nodeName", networkNode.getNodeName());
            fields.put("nodeType", networkNode.getNodeType());
            fields.put("region", networkNode.getRegion());
            fields.put("vendor", networkNode.getVendor());
            fields.put("active", networkNode.getActive());
            return fields;
        }

        if (entity instanceof MaintenanceWindow maintenanceWindow) {
            fields.put("eventDataset", "maintenance");
            fields.put("entityType", "MaintenanceWindow");
            fields.put("tableName", "maintenance_window");
            fields.put("entityId", maintenanceWindow.getId());
            fields.put("title", maintenanceWindow.getTitle());
            fields.put("maintenanceStatus", maintenanceWindow.getStatus());
            fields.put("startTime", maintenanceWindow.getStartTime());
            fields.put("endTime", maintenanceWindow.getEndTime());
            return fields;
        }

        if (entity instanceof MaintenanceNode maintenanceNode) {
            fields.put("eventDataset", "maintenance");
            fields.put("entityType", "MaintenanceNode");
            fields.put("tableName", "maintenance_node");
            fields.put("entityId", maintenanceNode.getId());
            fields.put("maintenanceWindowId", maintenanceNode.getMaintenanceWindow() != null ? maintenanceNode.getMaintenanceWindow().getId() : null);
            fields.put("networkNodeId", maintenanceNode.getNetworkNode() != null ? maintenanceNode.getNetworkNode().getId() : null);
            return fields;
        }

        if (entity instanceof AlarmEvent alarmEvent) {
            fields.put("eventDataset", "alarm");
            fields.put("entityType", "AlarmEvent");
            fields.put("tableName", "alarm_event");
            fields.put("entityId", alarmEvent.getId());
            fields.put("sourceSystem", alarmEvent.getSourceSystem());
            fields.put("externalId", alarmEvent.getExternalId());
            fields.put("networkNodeId", alarmEvent.getNetworkNode() != null ? alarmEvent.getNetworkNode().getId() : null);
            fields.put("incidentId", alarmEvent.getIncident() != null ? alarmEvent.getIncident().getId() : null);
            fields.put("alarmType", alarmEvent.getAlarmType());
            fields.put("severity", alarmEvent.getSeverity());
            fields.put("alarmStatus", alarmEvent.getStatus());
            fields.put("suppressedByMaintenance", alarmEvent.getSuppressedByMaintenance());
            return fields;
        }

        fields.put("eventDataset", "system");
        fields.put("entityType", entity.getClass().getSimpleName());
        fields.put("tableName", entity.getClass().getSimpleName());
        return fields;
    }
}
