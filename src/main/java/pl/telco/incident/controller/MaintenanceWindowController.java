package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowPageResponse;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.dto.MaintenanceWindowUpdateRequest;
import pl.telco.incident.entity.enums.MaintenanceStatus;
import pl.telco.incident.service.MaintenanceWindowService;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/maintenance-windows")
@RequiredArgsConstructor
@Tag(name = "Maintenance Windows")
public class MaintenanceWindowController {

    private final MaintenanceWindowService maintenanceWindowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create maintenance window", description = "Creates a maintenance window and links selected network nodes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Maintenance window created"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid time range",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public MaintenanceWindowResponse createMaintenanceWindow(@Valid @RequestBody MaintenanceWindowCreateRequest request) {
        return maintenanceWindowService.createMaintenanceWindow(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update maintenance window", description = "Partially updates a maintenance window and optionally replaces linked network nodes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance window updated"),
            @ApiResponse(responseCode = "400", description = "Validation error, invalid time range or empty patch",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Maintenance window or referenced network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public MaintenanceWindowResponse updateMaintenanceWindow(@PathVariable("id") Long id,
                                                             @Valid @RequestBody MaintenanceWindowUpdateRequest request) {
        return maintenanceWindowService.updateMaintenanceWindow(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance window by ID", description = "Returns a single maintenance window with linked network node identifiers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance window found"),
            @ApiResponse(responseCode = "404", description = "Maintenance window not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public MaintenanceWindowResponse getMaintenanceWindowById(@PathVariable("id") Long id) {
        return maintenanceWindowService.getMaintenanceWindowById(id);
    }

    @GetMapping
    @Operation(summary = "List maintenance windows", description = "Returns a paginated maintenance window list with optional filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance windows returned",
                    content = @Content(schema = @Schema(implementation = MaintenanceWindowPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filters or pagination values",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public MaintenanceWindowPageResponse getMaintenanceWindows(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 to 100", example = "10")
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Supported values: id, title, status, startTime, endTime", example = "startTime")
            @RequestParam(name = "sortBy", defaultValue = "startTime") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @Parameter(description = "Filter by a single maintenance status", example = "PLANNED")
            @RequestParam(name = "status", required = false) MaintenanceStatus status,
            @Parameter(description = "Filter by multiple maintenance statuses. Supports repeated params or comma-separated values.", example = "PLANNED,IN_PROGRESS")
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @Parameter(description = "Case-insensitive partial match on title", example = "core")
            @RequestParam(name = "title", required = false) String title,
            @Parameter(description = "Return only maintenance windows linked to this node", example = "5")
            @RequestParam(name = "nodeId", required = false) @Min(1) Long nodeId,
            @Parameter(description = "Include maintenance windows starting at or after this timestamp.", example = "2026-03-31T08:00:00")
            @RequestParam(name = "startFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @Parameter(description = "Include maintenance windows starting at or before this timestamp.", example = "2026-03-31T12:00:00")
            @RequestParam(name = "startTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @Parameter(description = "Include maintenance windows ending at or after this timestamp.", example = "2026-03-31T10:00:00")
            @RequestParam(name = "endFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endFrom,
            @Parameter(description = "Include maintenance windows ending at or before this timestamp.", example = "2026-03-31T14:00:00")
            @RequestParam(name = "endTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTo
    ) {
        return MaintenanceWindowPageResponse.from(maintenanceWindowService.getMaintenanceWindows(
                page,
                size,
                sortBy,
                direction,
                status,
                statuses,
                title,
                nodeId,
                startFrom,
                startTo,
                endFrom,
                endTo
        ));
    }
}
