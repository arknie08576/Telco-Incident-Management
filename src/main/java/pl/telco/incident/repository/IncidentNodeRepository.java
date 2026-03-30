package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;

public interface IncidentNodeRepository extends JpaRepository<IncidentNode, Long> {

    boolean existsByIncidentIdAndNetworkNodeId(Long incidentId, Long networkNodeId);

    boolean existsByIncidentIdAndNetworkNodeIdAndIdNot(Long incidentId, Long networkNodeId, Long id);

    boolean existsByIncidentIdAndRole(Long incidentId, IncidentNodeRole role);

    boolean existsByIncidentIdAndRoleAndIdNot(Long incidentId, IncidentNodeRole role, Long id);
}
