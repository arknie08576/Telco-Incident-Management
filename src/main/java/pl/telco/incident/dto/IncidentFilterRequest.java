package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;
import pl.telco.incident.validation.DateRangeValid;

import java.time.LocalDateTime;
import java.util.List;

@Data
@DateRangeValid(from = "openedFrom", to = "openedTo", message = "openedFrom must be earlier than or equal to openedTo")
@DateRangeValid(from = "acknowledgedFrom", to = "acknowledgedTo", message = "acknowledgedFrom must be earlier than or equal to acknowledgedTo")
@DateRangeValid(from = "resolvedFrom", to = "resolvedTo", message = "resolvedFrom must be earlier than or equal to resolvedTo")
@DateRangeValid(from = "closedFrom", to = "closedTo", message = "closedFrom must be earlier than or equal to closedTo")
public class IncidentFilterRequest {

    @Parameter(description = "Zero-based page index", example = "0")
    @Min(0)
    private int page = 0;

    @Parameter(description = "Page size, from 1 to 100", example = "10")
    @Min(1) @Max(100)
    private int size = 10;

    @Parameter(description = "Supported values: openedAt, acknowledgedAt, resolvedAt, closedAt, incidentNumber, priority, title", example = "openedAt")
    private String sortBy = "openedAt";

    @Parameter(description = "Sort direction", example = "desc")
    private String direction = "desc";

    @Parameter(description = "Filter by a single priority", example = "HIGH")
    private IncidentPriority priority;

    @Parameter(description = "Filter by multiple priorities. Supports repeated params or comma-separated values.", example = "HIGH,CRITICAL")
    private List<String> priorities;

    @Parameter(description = "Region filter", example = "MAZOWIECKIE")
    private Region region;

    @Parameter(description = "Filter incidents that may be planned work", example = "false")
    private Boolean possiblyPlanned;

    @Parameter(description = "Filter by a single lifecycle status", example = "OPEN")
    private IncidentStatus status;

    @Parameter(description = "Filter by multiple lifecycle statuses. Supports repeated params or comma-separated values.", example = "OPEN,ACKNOWLEDGED")
    private List<String> statuses;

    @Parameter(description = "Case-insensitive partial match on incident number", example = "INC-10")
    private String incidentNumber;

    @Parameter(description = "Case-insensitive partial match on title", example = "router failure")
    private String title;

    @Parameter(description = "Source alarm type filter", example = "HARDWARE")
    private SourceAlarmType sourceAlarmType;

    @Parameter(description = "Include incidents opened at or after this timestamp.", example = "2026-03-29T07:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openedFrom;

    @Parameter(description = "Include incidents opened at or before this timestamp.", example = "2026-03-29T12:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openedTo;

    @Parameter(description = "Include incidents acknowledged at or after this timestamp.", example = "2026-03-29T08:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime acknowledgedFrom;

    @Parameter(description = "Include incidents acknowledged at or before this timestamp.", example = "2026-03-29T09:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime acknowledgedTo;

    @Parameter(description = "Include incidents resolved at or after this timestamp.", example = "2026-03-29T09:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime resolvedFrom;

    @Parameter(description = "Include incidents resolved at or before this timestamp.", example = "2026-03-29T10:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime resolvedTo;

    @Parameter(description = "Include incidents closed at or after this timestamp.", example = "2026-03-29T10:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime closedFrom;

    @Parameter(description = "Include incidents closed at or before this timestamp.", example = "2026-03-29T11:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime closedTo;
}
