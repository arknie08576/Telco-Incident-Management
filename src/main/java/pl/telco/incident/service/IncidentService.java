package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request) {
        validateIncidentNumberUniqueness(request.getIncidentNumber());
        validateNodeUniqueness(request);
        validateRootNodeConsistency(request);

        NetworkNode rootNode = networkNodeRepository.findById(request.getRootNodeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Root node not found: " + request.getRootNodeId()
                ));

        Incident incident = new Incident();
        incident.setIncidentNumber(request.getIncidentNumber());
        incident.setTitle(request.getTitle());
        incident.setPriority(request.getPriority());
        incident.setRegion(request.getRegion());
        incident.setSourceAlarmType(request.getSourceAlarmType());
        incident.setPossiblyPlanned(request.getPossiblyPlanned());
        incident.setRootNode(rootNode);

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            NetworkNode networkNode = networkNodeRepository.findById(nodeRequest.getNetworkNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Network node not found: " + nodeRequest.getNetworkNodeId()
                    ));

            IncidentNode incidentNode = new IncidentNode();
            incidentNode.setNetworkNode(networkNode);
            incidentNode.setRole(nodeRequest.getRole());

            incident.addIncidentNode(incidentNode);
        }

        Incident saved = incidentRepository.save(incident);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateIncidentNumberUniqueness(String incidentNumber) {
        if (incidentRepository.findByIncidentNumber(incidentNumber).isPresent()) {
            throw new ConflictException("Incident with number already exists: " + incidentNumber);
        }
    }

    private void validateNodeUniqueness(IncidentCreateRequest request) {
        Set<Long> uniqueNodeIds = new HashSet<>();

        for (IncidentNodeRequest nodeRequest : request.getNodes()) {
            if (!uniqueNodeIds.add(nodeRequest.getNetworkNodeId())) {
                throw new BadRequestException(
                        "Duplicate networkNodeId in nodes: " + nodeRequest.getNetworkNodeId()
                );
            }
        }
    }

    private void validateRootNodeConsistency(IncidentCreateRequest request) {
        long rootCount = request.getNodes().stream()
                .filter(node -> node.getRole() == IncidentNodeRole.ROOT)
                .count();

        if (rootCount != 1) {
            throw new BadRequestException("Exactly one node must have role ROOT");
        }

        boolean rootMatches = request.getNodes().stream()
                .anyMatch(node ->
                        node.getRole() == IncidentNodeRole.ROOT
                                && request.getRootNodeId().equals(node.getNetworkNodeId())
                );

        if (!rootMatches) {
            throw new BadRequestException("rootNodeId must match the node with role ROOT");
        }
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