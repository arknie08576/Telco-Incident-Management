package pl.telco.incident.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.service.IncidentService;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident createIncident(@RequestBody IncidentCreateRequest request) {
        return incidentService.createIncident(request);
    }
}