package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentNodeRole;

@Schema(name = "IncidentNodeRequest", description = "Network node linked to the incident.")
@Data
public class IncidentNodeRequest {

    @Schema(description = "Identifier of a network node that already exists in inventory.", example = "10")
    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

    @Schema(description = "Role of the node in the incident graph.", example = "ROOT")
    @NotNull(message = "role is required")
    private IncidentNodeRole role;
}
