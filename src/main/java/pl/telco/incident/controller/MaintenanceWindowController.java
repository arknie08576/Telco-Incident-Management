package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import pl.telco.incident.dto.MaintenanceWindowRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.service.MaintenanceWindowService;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-windows")
@RequiredArgsConstructor
@Tag(name = "Maintenance Windows")
public class MaintenanceWindowController {

    private final MaintenanceWindowService maintenanceWindowService;

    @GetMapping
    @Operation(summary = "List maintenance windows")
    public List<MaintenanceWindowResponse> getAllMaintenanceWindows() {
        return maintenanceWindowService.getAllMaintenanceWindows();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance window by ID")
    public MaintenanceWindowResponse getMaintenanceWindowById(@PathVariable("id") Long id) {
        return maintenanceWindowService.getMaintenanceWindowById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create maintenance window")
    public MaintenanceWindowResponse createMaintenanceWindow(@Valid @RequestBody MaintenanceWindowRequest request) {
        return maintenanceWindowService.createMaintenanceWindow(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update maintenance window")
    public MaintenanceWindowResponse updateMaintenanceWindow(
            @PathVariable("id") Long id,
            @Valid @RequestBody MaintenanceWindowRequest request
    ) {
        return maintenanceWindowService.updateMaintenanceWindow(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete maintenance window")
    public void deleteMaintenanceWindow(@PathVariable("id") Long id) {
        maintenanceWindowService.deleteMaintenanceWindow(id);
    }
}
