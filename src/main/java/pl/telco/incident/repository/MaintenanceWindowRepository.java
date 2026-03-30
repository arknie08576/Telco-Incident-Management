package pl.telco.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.telco.incident.entity.MaintenanceWindow;

public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, Long> {
}
