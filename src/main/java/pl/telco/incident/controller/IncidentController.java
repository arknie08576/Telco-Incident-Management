package pl.telco.incident.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.service.IncidentService;

import java.util.List;

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

    @GetMapping
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }
}