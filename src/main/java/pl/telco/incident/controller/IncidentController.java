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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentPageResponse;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.service.IncidentService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create incident",
            description = "Creates a new incident with its root node and affected nodes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or malformed request",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Incident number already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse createIncident(@Valid @RequestBody IncidentCreateRequest request) {
        return incidentService.createIncident(request);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get incident by ID",
            description = "Returns a single incident with its current lifecycle timestamps."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse getIncidentById(@PathVariable("id") Long id) {
        return incidentService.getIncidentById(id);
    }

    @GetMapping
    @Operation(
            summary = "List incidents",
            description = "Returns a paginated incident list with optional filters for priority, region, planned work and status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Incident page returned",
                    content = @Content(schema = @Schema(implementation = IncidentPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Unsupported filters, invalid enums or invalid pagination values",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentPageResponse getAllIncidents(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 to 100", example = "10")
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Supported values: openedAt, incidentNumber, priority, title", example = "openedAt")
            @RequestParam(name = "sortBy", defaultValue = "openedAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @Parameter(description = "Filter by priority", example = "HIGH")
            @RequestParam(name = "priority", required = false) IncidentPriority priority,
            @Parameter(description = "Case-insensitive region filter", example = "MAZOWIECKIE")
            @RequestParam(name = "region", required = false) String region,
            @Parameter(description = "Filter incidents that may be planned work", example = "false")
            @RequestParam(name = "possiblyPlanned", required = false) Boolean possiblyPlanned,
            @Parameter(description = "Filter by lifecycle status", example = "OPEN")
            @RequestParam(name = "status", required = false) IncidentStatus status
    ) {
        Page<IncidentResponse> incidentPage = incidentService.getAllIncidents(
                page,
                size,
                sortBy,
                direction,
                priority,
                region,
                possiblyPlanned,
                status
        );

        return IncidentPageResponse.from(incidentPage);
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(
            summary = "Acknowledge incident",
            description = "Moves an incident from OPEN to ACKNOWLEDGED. Optional note is appended to timeline message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident acknowledged"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle transition or malformed request",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse acknowledgeIncident(
            @Parameter(description = "Incident identifier", example = "42")
            @PathVariable("id") Long id,
            @RequestBody(required = false) IncidentActionRequest request
    ) {
        return incidentService.acknowledgeIncident(id, request);
    }

    @PatchMapping("/{id}/resolve")
    @Operation(
            summary = "Resolve incident",
            description = "Moves an incident from ACKNOWLEDGED to RESOLVED. Optional note is appended to timeline message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident resolved"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle transition or malformed request",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse resolveIncident(
            @Parameter(description = "Incident identifier", example = "42")
            @PathVariable("id") Long id,
            @RequestBody(required = false) IncidentActionRequest request
    ) {
        return incidentService.resolveIncident(id, request);
    }

    @PatchMapping("/{id}/close")
    @Operation(
            summary = "Close incident",
            description = "Moves an incident from RESOLVED to CLOSED. Optional note is appended to timeline message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident closed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid lifecycle transition or malformed request",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse closeIncident(
            @Parameter(description = "Incident identifier", example = "42")
            @PathVariable("id") Long id,
            @RequestBody(required = false) IncidentActionRequest request
    ) {
        return incidentService.closeIncident(id, request);
    }

    @GetMapping("/{id}/timeline")
    @Operation(
            summary = "Get incident timeline",
            description = "Returns timeline events ordered by creation time ascending."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Timeline returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentTimelineResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public List<IncidentTimelineResponse> getIncidentTimeline(@PathVariable("id") Long id) {
        return incidentService.getIncidentTimeline(id);
    }
}
