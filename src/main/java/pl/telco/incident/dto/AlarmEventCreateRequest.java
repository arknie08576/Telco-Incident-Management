package pl.telco.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

@Data
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
}
