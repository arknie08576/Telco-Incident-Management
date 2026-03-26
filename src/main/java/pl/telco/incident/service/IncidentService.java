package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;

    public IncidentResponse createIncident(IncidentCreateRequest request) {
        Incident incident = new Incident();
        incident.setIncidentNumber(request.getIncidentNumber());
        incident.setTitle(request.getTitle());
        incident.setPriority(request.getPriority());
        incident.setRegion(request.getRegion());
        incident.setSourceAlarmType(request.getSourceAlarmType());
        incident.setPossiblyPlanned(request.getPossiblyPlanned());

        if (request.getRootNodeId() != null) {
            NetworkNode rootNode = networkNodeRepository.findById(request.getRootNodeId())
                    .orElseThrow(() -> new RuntimeException("Root node not found: " + request.getRootNodeId()));
            incident.setRootNode(rootNode);
        }

        if (request.getNodes() != null) {
            for (IncidentNodeRequest nodeRequest : request.getNodes()) {
                NetworkNode networkNode = networkNodeRepository.findById(nodeRequest.getNetworkNodeId())
                        .orElseThrow(() -> new RuntimeException(
                                "Network node not found: " + nodeRequest.getNetworkNodeId()
                        ));

                IncidentNode incidentNode = new IncidentNode();
                incidentNode.setNetworkNode(networkNode);
                incidentNode.setRole(nodeRequest.getRole());

                incident.addIncidentNode(incidentNode);
            }
        }

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