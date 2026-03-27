package pl.telco.incident.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.service.IncidentService;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return incidentService.getAllIncidents(page, size);
    }
}