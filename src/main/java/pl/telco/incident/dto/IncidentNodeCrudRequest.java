package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pl.telco.incident.entity.enums.IncidentNodeRole;

@Schema(name = "IncidentNodeCrudRequest", description = "Payload used to create or update an incident_node record directly.")
public class IncidentNodeCrudRequest {

    @Schema(description = "Referenced incident identifier.", example = "42")
    @NotNull(message = "incidentId is required")
    @Positive(message = "incidentId must be greater than 0")
    private Long incidentId;

    @Schema(description = "Referenced network node identifier.", example = "10")
    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

    @Schema(description = "Role of the node in the incident graph.", example = "AFFECTED")
    @NotNull(message = "role is required")
    private IncidentNodeRole role;

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
}
