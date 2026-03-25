package pl.telco.incident.dto;

import pl.telco.incident.entity.enums.IncidentPriority;

import java.util.List;

public class IncidentCreateRequest {

    private String incidentNumber;
    private String title;
    private IncidentPriority priority;
    private String region;
    private String sourceAlarmType;
    private Boolean possiblyPlanned;
    private Long rootNodeId;
    private List<IncidentNodeRequest> nodes;

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSourceAlarmType() {
        return sourceAlarmType;
    }

    public void setSourceAlarmType(String sourceAlarmType) {
        this.sourceAlarmType = sourceAlarmType;
    }

    public Boolean getPossiblyPlanned() {
        return possiblyPlanned;
    }

    public void setPossiblyPlanned(Boolean possiblyPlanned) {
        this.possiblyPlanned = possiblyPlanned;
    }

    public Long getRootNodeId() {
        return rootNodeId;
    }

    public void setRootNodeId(Long rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    public List<IncidentNodeRequest> getNodes() {
        return nodes;
    }

    public void setNodes(List<IncidentNodeRequest> nodes) {
        this.nodes = nodes;
    }
}