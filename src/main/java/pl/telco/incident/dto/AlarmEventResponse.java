package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

@Schema(name = "AlarmEventResponse", description = "Alarm event returned by the API.")
public class AlarmEventResponse {

    @Schema(description = "Database identifier.", example = "1")
    private Long id;

    @Schema(description = "Source monitoring system.", example = "OSS")
    private String sourceSystem;

    @Schema(description = "External alarm identifier.", example = "ALARM-100")
    private String externalId;

    @Schema(description = "Referenced network node identifier.", example = "1")
    private Long networkNodeId;

    @Schema(description = "Optional referenced incident identifier.", example = "42", nullable = true)
    private Long incidentId;

    @Schema(description = "Alarm type.", example = "LINK_DOWN")
    private String alarmType;

    @Schema(description = "Alarm severity.", example = "MAJOR")
    private AlarmSeverity severity;

    @Schema(description = "Alarm status.", example = "OPEN")
    private AlarmStatus status;

    @Schema(description = "Alarm description.", nullable = true)
    private String description;

    @Schema(description = "Whether the alarm was suppressed by maintenance.", example = "false")
    private Boolean suppressedByMaintenance;

    @Schema(description = "When the alarm occurred.", example = "2026-03-30T10:15:00")
    private LocalDateTime occurredAt;

    @Schema(description = "When the alarm was received.", example = "2026-03-30T10:16:00")
    private LocalDateTime receivedAt;

    @Schema(description = "Creation timestamp.", example = "2026-03-30T10:16:00")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getNetworkNodeId() {
        return networkNodeId;
    }

    public void setNetworkNodeId(Long networkNodeId) {
        this.networkNodeId = networkNodeId;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public AlarmSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlarmSeverity severity) {
        this.severity = severity;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getSuppressedByMaintenance() {
        return suppressedByMaintenance;
    }

    public void setSuppressedByMaintenance(Boolean suppressedByMaintenance) {
        this.suppressedByMaintenance = suppressedByMaintenance;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
