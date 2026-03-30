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
    @Operation(summary = "List incident_node rows")
    public List<IncidentNodeCrudResponse> getAllIncidentNodes() {
        return incidentNodeService.getAllIncidentNodes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident_node row by ID")
    public IncidentNodeCrudResponse getIncidentNodeById(@PathVariable("id") Long id) {
        return incidentNodeService.getIncidentNodeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create incident_node row")
    public IncidentNodeCrudResponse createIncidentNode(@Valid @RequestBody IncidentNodeCrudRequest request) {
        return incidentNodeService.createIncidentNode(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update incident_node row")
    public IncidentNodeCrudResponse updateIncidentNode(
            @PathVariable("id") Long id,
            @Valid @RequestBody IncidentNodeCrudRequest request
    ) {
        return incidentNodeService.updateIncidentNode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete incident_node row")
    public void deleteIncidentNode(@PathVariable("id") Long id) {
        incidentNodeService.deleteIncidentNode(id);
    }
}
