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
import pl.telco.incident.dto.IncidentTimelineEntryRequest;
import pl.telco.incident.dto.IncidentTimelineEntryResponse;
import pl.telco.incident.service.IncidentTimelineEntryService;

import java.util.List;

@RestController
@RequestMapping("/api/incident-timeline")
@RequiredArgsConstructor
@Tag(name = "Incident Timeline Entries")
public class IncidentTimelineEntryController {

    private final IncidentTimelineEntryService incidentTimelineEntryService;

    @GetMapping
    @Operation(
            summary = "List incident_timeline rows",
            description = "Returns raw incident_timeline rows. Use this API when you need direct table-level editing."
    )
    @ApiResponse(
            responseCode = "200",
            description = "incident_timeline rows returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentTimelineEntryResponse.class)))
    )
    public List<IncidentTimelineEntryResponse> getAllIncidentTimelineEntries() {
        return incidentTimelineEntryService.getAllIncidentTimelineEntries();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident_timeline row by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "incident_timeline row found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_timeline row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentTimelineEntryResponse getIncidentTimelineEntryById(@PathVariable("id") Long id) {
        return incidentTimelineEntryService.getIncidentTimelineEntryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create incident_timeline row")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "incident_timeline row created"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentTimelineEntryResponse createIncidentTimelineEntry(
            @Valid @RequestBody IncidentTimelineEntryRequest request
    ) {
        return incidentTimelineEntryService.createIncidentTimelineEntry(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update incident_timeline row")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "incident_timeline row updated"),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_timeline row or referenced incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public IncidentTimelineEntryResponse updateIncidentTimelineEntry(
            @PathVariable("id") Long id,
            @Valid @RequestBody IncidentTimelineEntryRequest request
    ) {
        return incidentTimelineEntryService.updateIncidentTimelineEntry(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete incident_timeline row")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "incident_timeline row deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "incident_timeline row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public void deleteIncidentTimelineEntry(@PathVariable("id") Long id) {
        incidentTimelineEntryService.deleteIncidentTimelineEntry(id);
    }
}
