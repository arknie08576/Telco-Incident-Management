package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.Region;

import java.time.LocalDateTime;

@Schema(name = "IncidentSummaryResponse", description = "Summary state of an incident used in paginated listings.")
public class IncidentSummaryResponse {

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

    @Schema(description = "Timestamp when the incident was opened.", example = "2026-03-29T06:42:00")
    private LocalDateTime openedAt;

    @Schema(description = "Timestamp when the incident was acknowledged.", example = "2026-03-29T06:50:00", nullable = true)
    private LocalDateTime acknowledgedAt;

    @Schema(description = "Timestamp when the incident was resolved.", example = "2026-03-29T07:05:00", nullable = true)
    private LocalDateTime resolvedAt;

    @Schema(description = "Timestamp when the incident was closed.", example = "2026-03-29T07:15:00", nullable = true)
    private LocalDateTime closedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
