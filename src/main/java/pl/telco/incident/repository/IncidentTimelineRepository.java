package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.IncidentTimeline;

import java.util.List;
import java.util.Optional;

public interface IncidentTimelineRepository extends JpaRepository<IncidentTimeline, Long> {

    List<IncidentTimeline> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    Optional<IncidentTimeline> findByIdAndIncidentId(Long id, Long incidentId);
}
