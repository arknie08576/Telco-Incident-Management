package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import pl.telco.incident.entity.enums.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaintenanceWindowFilterRequest {

    @Parameter(description = "Zero-based page index", example = "0")
    @Min(0)
    private int page = 0;

    @Parameter(description = "Page size, from 1 to 100", example = "10")
    @Min(1) @Max(100)
    private int size = 10;

    @Parameter(description = "Supported values: id, title, status, startTime, endTime", example = "startTime")
    private String sortBy = "startTime";

    @Parameter(description = "Sort direction", example = "desc")
    private String direction = "desc";

    @Parameter(description = "Filter by a single maintenance status", example = "PLANNED")
    private MaintenanceStatus status;

    @Parameter(description = "Filter by multiple maintenance statuses. Supports repeated params or comma-separated values.", example = "PLANNED,IN_PROGRESS")
    private List<String> statuses;

    @Parameter(description = "Case-insensitive partial match on title", example = "core")
    private String title;

    @Parameter(description = "Return only maintenance windows linked to this node", example = "5")
    @Min(1)
    private Long nodeId;

    @Parameter(description = "Include maintenance windows starting at or after this timestamp.", example = "2026-03-31T08:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startFrom;

    @Parameter(description = "Include maintenance windows starting at or before this timestamp.", example = "2026-03-31T12:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTo;

    @Parameter(description = "Include maintenance windows ending at or after this timestamp.", example = "2026-03-31T10:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endFrom;

    @Parameter(description = "Include maintenance windows ending at or before this timestamp.", example = "2026-03-31T14:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTo;
}
