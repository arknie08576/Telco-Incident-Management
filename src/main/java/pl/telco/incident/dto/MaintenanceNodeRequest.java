package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "MaintenanceNodeRequest", description = "Payload used to create or update a maintenance_node record directly.")
public class MaintenanceNodeRequest {

    @Schema(description = "Referenced maintenance window identifier.", example = "7")
    @NotNull(message = "maintenanceWindowId is required")
    @Positive(message = "maintenanceWindowId must be greater than 0")
    private Long maintenanceWindowId;

    @Schema(description = "Referenced network node identifier.", example = "10")
    @NotNull(message = "networkNodeId is required")
    @Positive(message = "networkNodeId must be greater than 0")
    private Long networkNodeId;

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
}
