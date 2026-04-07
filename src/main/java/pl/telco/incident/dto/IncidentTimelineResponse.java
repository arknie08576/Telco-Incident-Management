package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;

import java.time.LocalDateTime;

@Data
@Schema(name = "IncidentTimelineResponse", description = "Single timeline event related to an incident.")
public class IncidentTimelineResponse {

    @Schema(description = "Timeline event identifier.", example = "1001")
    private Long id;

    @Schema(description = "Timeline event type.", example = "RESOLVED")
    private IncidentTimelineEventType eventType;

    @Schema(description = "Human-readable timeline message.", example = "Incident resolved: Traffic rerouted")
    private String message;

    @Schema(description = "Timestamp when the event was created.", example = "2026-03-29T07:05:00")
    private LocalDateTime createdAt;
}
