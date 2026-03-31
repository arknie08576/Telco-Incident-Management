package pl.telco.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

public class AlarmEventCreateRequest {

    @NotBlank(message = "sourceSystem is required")
    @Size(max = 50, message = "sourceSystem must not exceed 50 characters")
    private String sourceSystem;

    @NotBlank(message = "externalId is required")
    @Size(max = 100, message = "externalId must not exceed 100 characters")
    private String externalId;

    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

    @Positive(message = "incidentId must be greater than 0")
    private Long incidentId;

    @NotBlank(message = "alarmType is required")
    @Size(max = 50, message = "alarmType must not exceed 50 characters")
    private String alarmType;

    @NotNull(message = "severity is required")
    private AlarmSeverity severity;

    @NotNull(message = "status is required")
    private AlarmStatus status;

    private String description;

    private Boolean suppressedByMaintenance;

    @NotNull(message = "occurredAt is required")
    private LocalDateTime occurredAt;

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
}
