package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

public class AlarmEventUpdateRequest {

    private AlarmSeverity severity;

    private AlarmStatus status;

    @Positive(message = "incidentId must be greater than 0")
    private Long incidentId;

    @Pattern(regexp = ".*\\S.*", message = "alarmType must not be blank")
    @Size(max = 50, message = "alarmType must not exceed 50 characters")
    private String alarmType;

    private String description;

    private Boolean suppressedByMaintenance;

    private LocalDateTime occurredAt;

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
