package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.validation.DateRangeValid;

import java.time.LocalDateTime;
import java.util.List;

@Data
@DateRangeValid(from = "occurredFrom", to = "occurredTo", message = "occurredFrom must be earlier than or equal to occurredTo")
@DateRangeValid(from = "receivedFrom", to = "receivedTo", message = "receivedFrom must be earlier than or equal to receivedTo")
public class AlarmEventFilterRequest {

    @Parameter(description = "Zero-based page index", example = "0")
    @Min(0)
    private int page = 0;

    @Parameter(description = "Page size, from 1 to 100", example = "10")
    @Min(1) @Max(100)
    private int size = 10;

    @Parameter(description = "Supported values: id, externalId, sourceSystem, alarmType, severity, status, occurredAt, receivedAt", example = "occurredAt")
    private String sortBy = "occurredAt";

    @Parameter(description = "Sort direction", example = "desc")
    private String direction = "desc";

    @Parameter(description = "Filter by a single severity", example = "MAJOR")
    private AlarmSeverity severity;

    @Parameter(description = "Filter by multiple severities. Supports repeated params or comma-separated values.", example = "MAJOR,CRITICAL")
    private List<String> severities;

    @Parameter(description = "Filter by a single alarm status", example = "OPEN")
    private AlarmStatus status;

    @Parameter(description = "Filter by multiple alarm statuses. Supports repeated params or comma-separated values.", example = "OPEN,ACKNOWLEDGED")
    private List<String> statuses;

    @Parameter(description = "Case-insensitive partial match on source system", example = "demo")
    private String sourceSystem;

    @Parameter(description = "Case-insensitive partial match on external ID", example = "ALARM-DEMO")
    private String externalId;

    @Parameter(description = "Case-insensitive partial match on alarm type", example = "BGP")
    private String alarmType;

    @Parameter(description = "Filter by network node ID", example = "5")
    @Min(1)
    private Long networkNodeId;

    @Parameter(description = "Filter by correlated incident ID", example = "42")
    @Min(1)
    private Long incidentId;

    @Parameter(description = "Filter by maintenance suppression flag", example = "false")
    private Boolean suppressedByMaintenance;

    @Parameter(description = "Include alarms that occurred at or after this timestamp.", example = "2026-03-31T08:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime occurredFrom;

    @Parameter(description = "Include alarms that occurred at or before this timestamp.", example = "2026-03-31T12:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime occurredTo;

    @Parameter(description = "Include alarms received at or after this timestamp.", example = "2026-03-31T08:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime receivedFrom;

    @Parameter(description = "Include alarms received at or before this timestamp.", example = "2026-03-31T12:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime receivedTo;
}
