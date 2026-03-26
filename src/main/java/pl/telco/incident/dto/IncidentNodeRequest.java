package pl.telco.incident.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentNodeRole;

@Data
public class IncidentNodeRequest {

    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

    @NotNull(message = "role is required")
    private IncidentNodeRole role;
}