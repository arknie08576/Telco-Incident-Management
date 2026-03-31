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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.service.MaintenanceWindowService;

import java.util.List;

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

    @GetMapping
    @Operation(summary = "List maintenance windows", description = "Returns maintenance windows with linked network node identifiers.")
    @ApiResponse(responseCode = "200", description = "Maintenance windows returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceWindowResponse.class))))
    public List<MaintenanceWindowResponse> getMaintenanceWindows() {
        return maintenanceWindowService.getMaintenanceWindows();
    }
}
