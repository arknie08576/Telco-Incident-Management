package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "IncidentTimelineResponse", description = "Single timeline event related to an incident.")
public class IncidentTimelineResponse {

    @Schema(description = "Timeline event identifier.", example = "1001")
    private Long id;
    @Schema(description = "Timeline event type.", example = "RESOLVED")
    private String eventType;
    @Schema(description = "Human-readable timeline message.", example = "Incident resolved: Traffic rerouted")
    private String message;
    @Schema(description = "Timestamp when the event was created.", example = "2026-03-29T07:05:00")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
