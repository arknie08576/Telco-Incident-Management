package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "IncidentResponse", description = "Detailed current state of an incident.")
public class IncidentResponse {

    @Schema(description = "Database identifier.", example = "42")
    private Long id;

    @Schema(description = "Unique incident number.", example = "INC-100")
    private String incidentNumber;

    @Schema(description = "Short incident title.", example = "Router failure in Warsaw")
    private String title;

    @Schema(description = "Current lifecycle status.", example = "OPEN")
    private IncidentStatus status;

    @Schema(description = "Business priority.", example = "HIGH")
    private IncidentPriority priority;

    @Schema(description = "Affected network region.", example = "MAZOWIECKIE")
    private Region region;

    @Schema(description = "Source alarm type reported by monitoring.", example = "HARDWARE", nullable = true)
    private SourceAlarmType sourceAlarmType;

    @Schema(description = "Marks whether the incident may be related to planned maintenance.", example = "false")
    private Boolean possiblyPlanned;

    @Schema(description = "Identifier of the root network node.", example = "1")
    private Long rootNodeId;

    @Schema(description = "Timestamp when the incident was opened.", example = "2026-03-29T06:42:00")
    private LocalDateTime openedAt;

    @Schema(description = "Timestamp when the incident was acknowledged.", example = "2026-03-29T06:50:00", nullable = true)
    private LocalDateTime acknowledgedAt;

    @Schema(description = "Timestamp when the incident was resolved.", example = "2026-03-29T07:05:00", nullable = true)
    private LocalDateTime resolvedAt;

    @Schema(description = "Timestamp when the incident was closed.", example = "2026-03-29T07:15:00", nullable = true)
    private LocalDateTime closedAt;

    @ArraySchema(schema = @Schema(implementation = IncidentNodeResponse.class))
    private List<IncidentNodeResponse> nodes;
}
