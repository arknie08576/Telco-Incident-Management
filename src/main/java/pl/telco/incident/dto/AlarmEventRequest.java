package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

@Schema(name = "AlarmEventRequest", description = "Payload used to create or update an alarm event.")
public class AlarmEventRequest {

    @Schema(description = "Source monitoring system.", example = "OSS")
    @NotBlank(message = "sourceSystem is required")
    @Size(max = 50, message = "sourceSystem must not exceed 50 characters")
    private String sourceSystem;

    @Schema(description = "External alarm identifier.", example = "ALARM-100")
    @NotBlank(message = "externalId is required")
    @Size(max = 100, message = "externalId must not exceed 100 characters")
    private String externalId;

    @Schema(description = "Referenced network node identifier.", example = "1")
    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

    @Schema(description = "Optional referenced incident identifier.", example = "42", nullable = true)
    @Positive(message = "incidentId must be greater than 0")
    private Long incidentId;

    @Schema(description = "Alarm type.", example = "LINK_DOWN")
    @NotBlank(message = "alarmType is required")
    @Size(max = 50, message = "alarmType must not exceed 50 characters")
    private String alarmType;

    @Schema(description = "Alarm severity.", example = "MAJOR")
    @NotNull(message = "severity is required")
    private AlarmSeverity severity;

    @Schema(description = "Alarm status.", example = "OPEN")
    @NotNull(message = "status is required")
    private AlarmStatus status;

    @Schema(description = "Alarm description.", nullable = true)
    private String description;

    @Schema(description = "Whether the alarm was suppressed by maintenance.", example = "false")
    @NotNull(message = "suppressedByMaintenance is required")
    private Boolean suppressedByMaintenance;

    @Schema(description = "When the alarm occurred.", example = "2026-03-30T10:15:00")
    @NotNull(message = "occurredAt is required")
    private LocalDateTime occurredAt;

    @Schema(description = "When the alarm was received by the platform.", example = "2026-03-30T10:16:00", nullable = true)
    private LocalDateTime receivedAt;

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
}
