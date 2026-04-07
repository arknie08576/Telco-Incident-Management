package pl.telco.incident.dto;

import lombok.Data;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;

@Data
public class AlarmEventResponse {

    private Long id;
    private String sourceSystem;
    private String externalId;
    private Long networkNodeId;
    private Long incidentId;
    private String alarmType;
    private AlarmSeverity severity;
    private AlarmStatus status;
    private String description;
    private Boolean suppressedByMaintenance;
    private LocalDateTime occurredAt;
    private LocalDateTime receivedAt;
}
