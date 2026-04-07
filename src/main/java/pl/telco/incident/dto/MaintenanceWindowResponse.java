package pl.telco.incident.dto;

import lombok.Data;
import pl.telco.incident.entity.enums.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaintenanceWindowResponse {

    private Long id;
    private String title;
    private String description;
    private MaintenanceStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Long> nodeIds;
}
