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
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentFilterRequest;
import pl.telco.incident.dto.IncidentPageResponse;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentUpdateRequest;
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
            description = "Returns a single incident with current lifecycle timestamps, root node and linked incident nodes."
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

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update incident",
            description = "Partially updates editable incident fields, including optional replacement of the root node and linked incident nodes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error, empty patch or editing not allowed for current incident state",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Incident number already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentResponse updateIncident(
            @Parameter(description = "Incident identifier", example = "42")
            @PathVariable("id") Long id,
            @Valid @RequestBody IncidentUpdateRequest request
    ) {
        return incidentService.updateIncident(id, request);
    }

    @GetMapping
    @Operation(
            summary = "List incidents",
            description = "Returns a paginated incident list with optional filters for lifecycle, text fields and date ranges."
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
    public IncidentPageResponse getAllIncidents(@ParameterObject @ModelAttribute @Valid IncidentFilterRequest filter) {
        return IncidentPageResponse.from(incidentService.getAllIncidents(filter));
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
