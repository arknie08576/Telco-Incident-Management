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
import pl.telco.incident.dto.MaintenanceNodeRequest;
import pl.telco.incident.dto.MaintenanceNodeResponse;
import pl.telco.incident.service.MaintenanceNodeService;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-nodes")
@RequiredArgsConstructor
@Tag(name = "Maintenance Nodes")
public class MaintenanceNodeController {

    private final MaintenanceNodeService maintenanceNodeService;

    @GetMapping
    @Operation(
            summary = "List maintenance_node rows",
            description = "Returns raw maintenance-node relation rows. Use this API when you need direct table-level editing."
    )
    @ApiResponse(
            responseCode = "200",
            description = "maintenance_node rows returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceNodeResponse.class)))
    )
    public List<MaintenanceNodeResponse> getAllMaintenanceNodes() {
        return maintenanceNodeService.getAllMaintenanceNodes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance_node row by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "maintenance_node row found"),
            @ApiResponse(
                    responseCode = "404",
                    description = "maintenance_node row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public MaintenanceNodeResponse getMaintenanceNodeById(@PathVariable("id") Long id) {
        return maintenanceNodeService.getMaintenanceNodeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create maintenance_node row")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "maintenance_node row created"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced maintenance window or network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Relation already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public MaintenanceNodeResponse createMaintenanceNode(@Valid @RequestBody MaintenanceNodeRequest request) {
        return maintenanceNodeService.createMaintenanceNode(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update maintenance_node row")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "maintenance_node row updated"),
            @ApiResponse(
                    responseCode = "404",
                    description = "maintenance_node row or referenced entities not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Relation already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public MaintenanceNodeResponse updateMaintenanceNode(
            @PathVariable("id") Long id,
            @Valid @RequestBody MaintenanceNodeRequest request
    ) {
        return maintenanceNodeService.updateMaintenanceNode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete maintenance_node row")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "maintenance_node row deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "maintenance_node row not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public void deleteMaintenanceNode(@PathVariable("id") Long id) {
        maintenanceNodeService.deleteMaintenanceNode(id);
    }
}
