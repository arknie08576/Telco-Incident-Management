package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

@Data
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
}
