package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "IncidentTimelineEntryRequest", description = "Payload used to create or update an incident_timeline record directly.")
public class IncidentTimelineEntryRequest {

    @Schema(description = "Referenced incident identifier.", example = "42")
    @NotNull(message = "incidentId is required")
    @Positive(message = "incidentId must be greater than 0")
    private Long incidentId;

    @Schema(description = "Timeline event type.", example = "MANUAL_NOTE")
    @NotBlank(message = "eventType is required")
    @Size(max = 50, message = "eventType must not exceed 50 characters")
    private String eventType;

    @Schema(description = "Human-readable timeline message.", example = "Operator added manual note")
    @NotBlank(message = "message is required")
    private String message;

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

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
