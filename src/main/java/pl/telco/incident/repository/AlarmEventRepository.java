package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.AlarmEvent;

import java.util.Optional;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {

    Optional<AlarmEvent> findBySourceSystemAndExternalId(String sourceSystem, String externalId);

    boolean existsBySourceSystemAndExternalId(String sourceSystem, String externalId);

    boolean existsBySourceSystemAndExternalIdAndIdNot(String sourceSystem, String externalId, Long id);

    long countByIncidentId(Long incidentId);
}
