package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.List;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds) {
        this.nodeIds = nodeIds;
    }
}
