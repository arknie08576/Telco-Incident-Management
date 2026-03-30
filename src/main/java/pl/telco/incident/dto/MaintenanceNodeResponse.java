package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MaintenanceNodeResponse", description = "Single maintenance_node record exposed as a standalone API resource.")
public class MaintenanceNodeResponse {

    @Schema(description = "Technical identifier of the maintenance_node row.", example = "2001")
    private Long id;

    @Schema(description = "Referenced maintenance window identifier.", example = "7")
    private Long maintenanceWindowId;

    @Schema(description = "Referenced network node identifier.", example = "10")
    private Long networkNodeId;

    @Schema(description = "Timestamp when the relation row was created.", example = "2026-03-30T09:15:00")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaintenanceWindowId() {
        return maintenanceWindowId;
    }

    public void setMaintenanceWindowId(Long maintenanceWindowId) {
        this.maintenanceWindowId = maintenanceWindowId;
    }

    public Long getNetworkNodeId() {
        return networkNodeId;
    }

    public void setNetworkNodeId(Long networkNodeId) {
        this.networkNodeId = networkNodeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
