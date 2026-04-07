package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "IncidentActionRequest", description = "Optional note attached to lifecycle actions.")
public class IncidentActionRequest {

    @Schema(description = "Optional operator note appended to the lifecycle timeline entry.", example = "Traffic rerouted")
    private String note;
}
