package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.IncidentNodeCrudRequest;
import pl.telco.incident.dto.IncidentNodeCrudResponse;
import pl.telco.incident.service.IncidentNodeService;

import java.util.List;

@RestController
@RequestMapping("/api/incident-nodes")
@RequiredArgsConstructor
@Tag(name = "Incident Nodes")
public class IncidentNodeController {

    private final IncidentNodeService incidentNodeService;

    @GetMapping
    @Operation(
            summary = "List incident_node rows",
            description = "Returns raw incident-node relation rows. Use this API when you need direct table-level editing."
    )
    @ApiResponse(
            responseCode = "200",
            description = "incident_node rows returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentNodeCrudResponse.class)))
    )
    public List<IncidentNodeCrudResponse> getAllIncidentNodes() {
        return incidentNodeService.getAllIncidentNodes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident_node row by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "incident_node row found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_node row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentNodeCrudResponse getIncidentNodeById(@PathVariable("id") Long id) {
        return incidentNodeService.getIncidentNodeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create incident_node row",
            description = "Creates a direct relation row between incident and network node while preserving incident ROOT rules."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "incident_node row created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid direct table operation for incident graph consistency",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced incident or network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Relation already exists or incident already has a different ROOT node",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentNodeCrudResponse createIncidentNode(@Valid @RequestBody IncidentNodeCrudRequest request) {
        return incidentNodeService.createIncidentNode(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update incident_node row",
            description = "Updates a direct incident-node relation row while preserving incident ROOT rules."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "incident_node row updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid direct table operation for incident graph consistency",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_node row or referenced entities not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Relation already exists or incident already has a different ROOT node",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentNodeCrudResponse updateIncidentNode(
            @PathVariable("id") Long id,
            @Valid @RequestBody IncidentNodeCrudRequest request
    ) {
        return incidentNodeService.updateIncidentNode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete incident_node row",
            description = "Deletes a direct incident-node relation row. ROOT rows cannot be deleted directly."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "incident_node row deleted"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Attempt to delete ROOT row directly",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_node row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public void deleteIncidentNode(@PathVariable("id") Long id) {
        incidentNodeService.deleteIncidentNode(id);
    }
}
