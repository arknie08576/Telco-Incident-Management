package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.IncidentNodeRole;

@Schema(name = "IncidentNodeResponse", description = "Network node linked to an incident.")
public class IncidentNodeResponse {

    @Schema(description = "Incident node identifier.", example = "501")
    private Long id;

    @Schema(description = "Referenced network node identifier.", example = "10")
    private Long networkNodeId;

    @Schema(description = "Role of the node in the incident graph.", example = "ROOT")
    private IncidentNodeRole role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
