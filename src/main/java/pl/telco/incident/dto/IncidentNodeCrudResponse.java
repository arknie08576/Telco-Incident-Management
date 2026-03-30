package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.IncidentNodeRole;

import java.time.LocalDateTime;

@Schema(name = "IncidentNodeCrudResponse", description = "Single incident_node record exposed as a standalone API resource.")
public class IncidentNodeCrudResponse {

    @Schema(description = "Technical identifier of the incident_node row.", example = "1001")
    private Long id;

    @Schema(description = "Referenced incident identifier.", example = "42")
    private Long incidentId;

    @Schema(description = "Referenced network node identifier.", example = "10")
    private Long networkNodeId;

    @Schema(description = "Role of the node in the incident graph.", example = "ROOT")
    private IncidentNodeRole role;

    @Schema(description = "Timestamp when the relation row was created.", example = "2026-03-30T09:15:00")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getNetworkNodeId() {
        return networkNodeId;
    }

    public void setNetworkNodeId(Long networkNodeId) {
        this.networkNodeId = networkNodeId;
    }

    public IncidentNodeRole getRole() {
        return role;
    }

    public void setRole(IncidentNodeRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
