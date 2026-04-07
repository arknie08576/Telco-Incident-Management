package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;

import java.util.List;

@Data
@Schema(name = "IncidentCreateRequest", description = "Payload used to create a new incident.")
public class IncidentCreateRequest {

    @Schema(description = "Unique incident number visible to operators.", example = "INC-100")
    @NotBlank(message = "incidentNumber is required")
    @Size(max = 50, message = "incidentNumber must not exceed 50 characters")
    private String incidentNumber;

    @Schema(description = "Short incident title.", example = "Router failure in Warsaw")
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Schema(description = "Business priority of the incident.", example = "HIGH")
    @NotNull(message = "priority is required")
    private IncidentPriority priority;

    @Schema(description = "Affected network region.", example = "MAZOWIECKIE")
    @NotNull(message = "region is required")
    private Region region;

    @Schema(description = "Source alarm type reported by monitoring.", example = "HARDWARE")
    private SourceAlarmType sourceAlarmType;

    @Schema(description = "Marks whether the incident may be related to planned maintenance.", example = "false")
    private Boolean possiblyPlanned;

    @Schema(description = "Identifier of the root network node.", example = "1")
    @NotNull(message = "rootNodeId is required")
    @Positive(message = "rootNodeId must be greater than 0")
    private Long rootNodeId;

    @ArraySchema(
            schema = @Schema(implementation = IncidentNodeRequest.class),
            minItems = 1,
            arraySchema = @Schema(description = "Incident nodes including exactly one ROOT node.")
    )
    @NotEmpty(message = "nodes must not be empty")
    @Valid
    private List<IncidentNodeRequest> nodes;
}
