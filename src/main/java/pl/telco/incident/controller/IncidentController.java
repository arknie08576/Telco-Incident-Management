package pl.telco.incident.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.service.IncidentService;

@Validated
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(@Valid @RequestBody IncidentCreateRequest request) {
        return incidentService.createIncident(request);
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @GetMapping
    public Page<IncidentResponse> getAllIncidents(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(name = "sortBy", defaultValue = "openedAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "priority", required = false) IncidentPriority priority,
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "possiblyPlanned", required = false) Boolean possiblyPlanned,
            @RequestParam(name = "status", required = false) IncidentStatus status
    ) {
        return incidentService.getAllIncidents(
                page,
                size,
                sortBy,
                direction,
                priority,
                region,
                possiblyPlanned,
                status
        );
    }
}