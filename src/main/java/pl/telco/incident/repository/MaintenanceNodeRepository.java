package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.MaintenanceNode;

public interface MaintenanceNodeRepository extends JpaRepository<MaintenanceNode, Long> {

    boolean existsByMaintenanceWindowIdAndNetworkNodeId(Long maintenanceWindowId, Long networkNodeId);

    boolean existsByMaintenanceWindowIdAndNetworkNodeIdAndIdNot(Long maintenanceWindowId, Long networkNodeId, Long id);
}
