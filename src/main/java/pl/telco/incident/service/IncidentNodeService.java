package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.IncidentNodeCrudRequest;
import pl.telco.incident.dto.IncidentNodeCrudResponse;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.repository.IncidentNodeRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IncidentNodeService {

    private final IncidentNodeRepository incidentNodeRepository;
    private final IncidentRepository incidentRepository;
    private final NetworkNodeRepository networkNodeRepository;

    @Transactional(readOnly = true)
    public List<IncidentNodeCrudResponse> getAllIncidentNodes() {
        return incidentNodeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentNodeCrudResponse getIncidentNodeById(Long id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Transactional
    public IncidentNodeCrudResponse createIncidentNode(IncidentNodeCrudRequest request) {
        Incident incident = findIncidentByIdOrThrow(request.getIncidentId());
        NetworkNode networkNode = findNetworkNodeByIdOrThrow(request.getNetworkNodeId());

        validateUniquePair(request.getIncidentId(), request.getNetworkNodeId(), null);
        validateCreateDomainRules(incident, request.getRole());

        IncidentNode incidentNode = new IncidentNode();
        incidentNode.setIncident(incident);
        incidentNode.setNetworkNode(networkNode);
        incidentNode.setRole(request.getRole());

        if (request.getRole() == IncidentNodeRole.ROOT) {
            incident.setRootNode(networkNode);
        }

        return mapToResponse(incidentNodeRepository.save(incidentNode));
    }

    @Transactional
    public IncidentNodeCrudResponse updateIncidentNode(Long id, IncidentNodeCrudRequest request) {
        IncidentNode incidentNode = findByIdOrThrow(id);
        Incident currentIncident = incidentNode.getIncident();
        Incident targetIncident = findIncidentByIdOrThrow(request.getIncidentId());
        NetworkNode targetNetworkNode = findNetworkNodeByIdOrThrow(request.getNetworkNodeId());
        IncidentNodeRole targetRole = request.getRole();

        validateUniquePair(targetIncident.getId(), targetNetworkNode.getId(), id);
        validateUpdateDomainRules(incidentNode, currentIncident, targetIncident, targetRole);

        incidentNode.setIncident(targetIncident);
        incidentNode.setNetworkNode(targetNetworkNode);
        incidentNode.setRole(targetRole);

        if (targetRole == IncidentNodeRole.ROOT) {
            targetIncident.setRootNode(targetNetworkNode);
        }

        return mapToResponse(incidentNodeRepository.save(incidentNode));
    }

    @Transactional
    public void deleteIncidentNode(Long id) {
        IncidentNode incidentNode = findByIdOrThrow(id);

        if (incidentNode.getRole() == IncidentNodeRole.ROOT) {
            throw new BadRequestException("ROOT incident node cannot be deleted directly");
        }

        incidentNodeRepository.delete(incidentNode);
    }

    private IncidentNode findByIdOrThrow(Long id) {
        return incidentNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident node not found: " + id));
    }

    private Incident findIncidentByIdOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private NetworkNode findNetworkNodeByIdOrThrow(Long id) {
        return networkNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network node not found: " + id));
    }

    private void validateUniquePair(Long incidentId, Long networkNodeId, Long currentId) {
        boolean exists = currentId == null
                ? incidentNodeRepository.existsByIncidentIdAndNetworkNodeId(incidentId, networkNodeId)
                : incidentNodeRepository.existsByIncidentIdAndNetworkNodeIdAndIdNot(incidentId, networkNodeId, currentId);

        if (exists) {
            throw new ConflictException(
                    "Incident node relation already exists for incidentId/networkNodeId: %d/%d"
                            .formatted(incidentId, networkNodeId)
            );
        }
    }

    private void validateCreateDomainRules(Incident incident, IncidentNodeRole role) {
        if (role == IncidentNodeRole.ROOT) {
            if (incidentNodeRepository.existsByIncidentIdAndRole(incident.getId(), IncidentNodeRole.ROOT)) {
                throw new ConflictException("Incident already has a ROOT node: " + incident.getId());
            }
            return;
        }

        if (!incidentNodeRepository.existsByIncidentIdAndRole(incident.getId(), IncidentNodeRole.ROOT)) {
            throw new BadRequestException("Incident must have a ROOT node before adding non-root nodes");
        }
    }

    private void validateUpdateDomainRules(
            IncidentNode existingNode,
            Incident currentIncident,
            Incident targetIncident,
            IncidentNodeRole targetRole
    ) {
        boolean sameIncident = Objects.equals(currentIncident.getId(), targetIncident.getId());
        boolean wasRoot = existingNode.getRole() == IncidentNodeRole.ROOT;
        boolean willBeRoot = targetRole == IncidentNodeRole.ROOT;

        if (wasRoot && (!sameIncident || !willBeRoot)) {
            throw new BadRequestException("ROOT incident node cannot be moved or demoted directly");
        }

        if (willBeRoot && incidentNodeRepository.existsByIncidentIdAndRoleAndIdNot(
                targetIncident.getId(),
                IncidentNodeRole.ROOT,
                existingNode.getId()
        )) {
            throw new ConflictException("Incident already has a different ROOT node: " + targetIncident.getId());
        }

        if (!willBeRoot && !incidentNodeRepository.existsByIncidentIdAndRoleAndIdNot(
                targetIncident.getId(),
                IncidentNodeRole.ROOT,
                existingNode.getId()
        )) {
            throw new BadRequestException("Incident must keep exactly one ROOT node");
        }
    }

    private IncidentNodeCrudResponse mapToResponse(IncidentNode incidentNode) {
        IncidentNodeCrudResponse response = new IncidentNodeCrudResponse();
        response.setId(incidentNode.getId());
        response.setIncidentId(incidentNode.getIncident().getId());
        response.setNetworkNodeId(incidentNode.getNetworkNode().getId());
        response.setRole(incidentNode.getRole());
        response.setCreatedAt(incidentNode.getCreatedAt());
        return response;
    }
}
