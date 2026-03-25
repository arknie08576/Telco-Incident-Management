package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;
import pl.telco.incident.dto.IncidentResponse;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;

    public IncidentResponse createIncident(IncidentCreateRequest request) {
        Incident incident = new Incident();
        // ... reszta jak była

        Incident saved = incidentRepository.save(incident);

        return mapToResponse(saved);
    }

    private IncidentResponse mapToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setIncidentNumber(incident.getIncidentNumber());
        response.setTitle(incident.getTitle());
        response.setStatus(incident.getStatus());
        response.setPriority(incident.getPriority());
        response.setRegion(incident.getRegion());
        response.setOpenedAt(incident.getOpenedAt());
        return response;
    }
}