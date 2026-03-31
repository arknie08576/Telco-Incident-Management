package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventPageResponse;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.dto.AlarmEventUpdateRequest;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.service.AlarmEventService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/alarm-events")
@RequiredArgsConstructor
@Tag(name = "Alarm Events")
public class AlarmEventController {

    private final AlarmEventService alarmEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create alarm event", description = "Creates an alarm event and optionally correlates it with an incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alarm event created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced incident or network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Alarm event already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse createAlarmEvent(@Valid @RequestBody AlarmEventCreateRequest request) {
        return alarmEventService.createAlarmEvent(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update alarm event", description = "Partially updates editable alarm fields such as status, incident correlation and maintenance suppression.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm event updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or empty patch",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alarm event or referenced incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse updateAlarmEvent(@PathVariable("id") Long id,
                                               @Valid @RequestBody AlarmEventUpdateRequest request) {
        return alarmEventService.updateAlarmEvent(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alarm event by ID", description = "Returns a single alarm event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm event found"),
            @ApiResponse(responseCode = "404", description = "Alarm event not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse getAlarmEventById(@PathVariable("id") Long id) {
        return alarmEventService.getAlarmEventById(id);
    }

    @GetMapping
    @Operation(summary = "List alarm events", description = "Returns a paginated alarm event list with optional filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm events returned",
                    content = @Content(schema = @Schema(implementation = AlarmEventPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filters or pagination values",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventPageResponse getAlarmEvents(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 to 100", example = "10")
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Supported values: id, externalId, sourceSystem, alarmType, severity, status, occurredAt, receivedAt", example = "occurredAt")
            @RequestParam(name = "sortBy", defaultValue = "occurredAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @Parameter(description = "Filter by a single severity", example = "MAJOR")
            @RequestParam(name = "severity", required = false) AlarmSeverity severity,
            @Parameter(description = "Filter by multiple severities. Supports repeated params or comma-separated values.", example = "MAJOR,CRITICAL")
            @RequestParam(name = "severities", required = false) List<String> severities,
            @Parameter(description = "Filter by a single alarm status", example = "OPEN")
            @RequestParam(name = "status", required = false) AlarmStatus status,
            @Parameter(description = "Filter by multiple alarm statuses. Supports repeated params or comma-separated values.", example = "OPEN,ACKNOWLEDGED")
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @Parameter(description = "Case-insensitive partial match on source system", example = "demo")
            @RequestParam(name = "sourceSystem", required = false) String sourceSystem,
            @Parameter(description = "Case-insensitive partial match on external ID", example = "ALARM-DEMO")
            @RequestParam(name = "externalId", required = false) String externalId,
            @Parameter(description = "Case-insensitive partial match on alarm type", example = "BGP")
            @RequestParam(name = "alarmType", required = false) String alarmType,
            @Parameter(description = "Filter by network node ID", example = "5")
            @RequestParam(name = "networkNodeId", required = false) @Min(1) Long networkNodeId,
            @Parameter(description = "Filter by correlated incident ID", example = "42")
            @RequestParam(name = "incidentId", required = false) @Min(1) Long incidentId,
            @Parameter(description = "Filter by maintenance suppression flag", example = "false")
            @RequestParam(name = "suppressedByMaintenance", required = false) Boolean suppressedByMaintenance,
            @Parameter(description = "Include alarms that occurred at or after this timestamp.", example = "2026-03-31T08:00:00")
            @RequestParam(name = "occurredFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredFrom,
            @Parameter(description = "Include alarms that occurred at or before this timestamp.", example = "2026-03-31T12:00:00")
            @RequestParam(name = "occurredTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredTo,
            @Parameter(description = "Include alarms received at or after this timestamp.", example = "2026-03-31T08:00:00")
            @RequestParam(name = "receivedFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedFrom,
            @Parameter(description = "Include alarms received at or before this timestamp.", example = "2026-03-31T12:00:00")
            @RequestParam(name = "receivedTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedTo
    ) {
        return AlarmEventPageResponse.from(alarmEventService.getAlarmEvents(
                page,
                size,
                sortBy,
                direction,
                severity,
                severities,
                status,
                statuses,
                sourceSystem,
                externalId,
                alarmType,
                networkNodeId,
                incidentId,
                suppressedByMaintenance,
                occurredFrom,
                occurredTo,
                receivedFrom,
                receivedTo
        ));
    }
}
