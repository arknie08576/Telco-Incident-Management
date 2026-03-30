package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "IncidentTimelineUpsertRequest", description = "Payload used to create or update a manual incident timeline event.")
public class IncidentTimelineUpsertRequest {

    @Schema(description = "Timeline event type.", example = "MANUAL_NOTE")
    @NotBlank(message = "eventType is required")
    @Size(max = 50, message = "eventType must not exceed 50 characters")
    private String eventType;

    @Schema(description = "Human-readable timeline message.", example = "Operator added manual note")
    @NotBlank(message = "message is required")
    private String message;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
