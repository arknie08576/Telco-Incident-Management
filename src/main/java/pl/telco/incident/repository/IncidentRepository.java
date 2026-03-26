package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.Incident;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    List<Incident> findAllByOrderByOpenedAtDesc();
}