package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "IncidentTimelineEntryResponse", description = "Single incident_timeline record exposed as a standalone API resource.")
public class IncidentTimelineEntryResponse {

    @Schema(description = "Technical identifier of the timeline row.", example = "3001")
    private Long id;

    @Schema(description = "Referenced incident identifier.", example = "42")
    private Long incidentId;

    @Schema(description = "Timeline event type.", example = "RESOLVED")
    private String eventType;

    @Schema(description = "Human-readable timeline message.", example = "Incident resolved: Traffic rerouted")
    private String message;

    @Schema(description = "Timestamp when the event was created.", example = "2026-03-30T09:15:00")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
