package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IncidentActionRequest", description = "Optional note attached to lifecycle actions.")
public class IncidentActionRequest {

    @Schema(description = "Optional operator note appended to the lifecycle timeline entry.", example = "Traffic rerouted")
    private String note;
}
