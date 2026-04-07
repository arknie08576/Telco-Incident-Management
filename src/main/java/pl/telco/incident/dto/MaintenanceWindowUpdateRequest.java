package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaintenanceWindowUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "title must not be blank")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String description;

    private MaintenanceStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Size(min = 1, message = "nodeIds must not be empty")
    private List<@Positive(message = "nodeIds must contain positive values") Long> nodeIds;
}
