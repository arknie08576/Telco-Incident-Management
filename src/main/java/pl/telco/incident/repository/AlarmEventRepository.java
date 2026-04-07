package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.telco.incident.entity.AlarmEvent;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long>, JpaSpecificationExecutor<AlarmEvent> {

    boolean existsBySourceSystemAndExternalId(String sourceSystem, String externalId);
}
