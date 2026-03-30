package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.MaintenanceWindowStatus;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "MaintenanceWindowRequest", description = "Payload used to create or update a maintenance window.")
public class MaintenanceWindowRequest {

    @Schema(description = "Maintenance title.", example = "Planned RAN upgrade - Krakow")
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Schema(description = "Maintenance description.", example = "Software upgrade for LTE/5G access nodes", nullable = true)
    private String description;

    @Schema(description = "Maintenance status.", example = "PLANNED")
    @NotNull(message = "status is required")
    private MaintenanceWindowStatus status;

    @Schema(description = "Planned start time.", example = "2026-03-31T08:00:00")
    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @Schema(description = "Planned end time.", example = "2026-03-31T12:00:00")
    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    @ArraySchema(schema = @Schema(implementation = Long.class), arraySchema = @Schema(description = "Affected network node identifiers."))
    @NotEmpty(message = "networkNodeIds must not be empty")
    private List<Long> networkNodeIds;

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

    public List<Long> getNetworkNodeIds() {
        return networkNodeIds;
    }

    public void setNetworkNodeIds(List<Long> networkNodeIds) {
        this.networkNodeIds = networkNodeIds;
    }
}
