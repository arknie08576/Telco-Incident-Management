package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.telco.incident.entity.Incident;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    boolean existsByIncidentNumberAndIdNot(String incidentNumber, Long id);
}
