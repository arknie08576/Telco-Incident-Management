package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.MaintenanceWindowStatus;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "MaintenanceWindowResponse", description = "Maintenance window returned by the API.")
public class MaintenanceWindowResponse {

    @Schema(description = "Database identifier.", example = "1")
    private Long id;

    @Schema(description = "Maintenance title.", example = "Planned RAN upgrade - Krakow")
    private String title;

    @Schema(description = "Maintenance description.", nullable = true)
    private String description;

    @Schema(description = "Maintenance status.", example = "PLANNED")
    private MaintenanceWindowStatus status;

    @Schema(description = "Planned start time.", example = "2026-03-31T08:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Planned end time.", example = "2026-03-31T12:00:00")
    private LocalDateTime endTime;

    @Schema(description = "Creation timestamp.", example = "2026-03-30T09:00:00")
    private LocalDateTime createdAt;

    @ArraySchema(schema = @Schema(implementation = Long.class))
    private List<Long> networkNodeIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public MaintenanceWindowStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceWindowStatus status) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Long> getNetworkNodeIds() {
        return networkNodeIds;
    }

    public void setNetworkNodeIds(List<Long> networkNodeIds) {
        this.networkNodeIds = networkNodeIds;
    }
}
