package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowFilterRequest;
import pl.telco.incident.dto.MaintenanceWindowPageResponse;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.dto.MaintenanceWindowUpdateRequest;
import pl.telco.incident.service.MaintenanceWindowService;

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
    public MaintenanceWindowResponse updateMaintenanceWindow(
            @Parameter(description = "Maintenance window identifier", example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody MaintenanceWindowUpdateRequest request
    ) {
        return maintenanceWindowService.updateMaintenanceWindow(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance window by ID", description = "Returns a single maintenance window with linked network node identifiers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance window found"),
            @ApiResponse(responseCode = "404", description = "Maintenance window not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public MaintenanceWindowResponse getMaintenanceWindowById(
            @Parameter(description = "Maintenance window identifier", example = "1")
            @PathVariable("id") Long id
    ) {
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
    public MaintenanceWindowPageResponse getMaintenanceWindows(@ParameterObject @ModelAttribute @Valid MaintenanceWindowFilterRequest filter) {
        return MaintenanceWindowPageResponse.from(maintenanceWindowService.getMaintenanceWindows(filter));
    }
}
