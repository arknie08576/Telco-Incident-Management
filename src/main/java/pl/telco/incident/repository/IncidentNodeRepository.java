package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.IncidentNode;

public interface IncidentNodeRepository extends JpaRepository<IncidentNode, Long> {
}