package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.telco.incident.entity.MaintenanceWindow;

public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, Long>, JpaSpecificationExecutor<MaintenanceWindow> {
}
