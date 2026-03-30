package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.NodeType;

@Schema(name = "NetworkNodeRequest", description = "Payload used to create or update a network node.")
public class NetworkNodeRequest {

    @Schema(description = "Unique network node name.", example = "CORE-RTR-WAW-02")
    @NotBlank(message = "nodeName is required")
    @Size(max = 100, message = "nodeName must not exceed 100 characters")
    private String nodeName;

    @Schema(description = "Network node type.", example = "ROUTER")
    @NotNull(message = "nodeType is required")
    private NodeType nodeType;

    @Schema(description = "Operational region.", example = "MAZOWIECKIE")
    @NotBlank(message = "region is required")
    @Size(max = 100, message = "region must not exceed 100 characters")
    private String region;

    @Schema(description = "Vendor name.", example = "Cisco", nullable = true)
    @Pattern(regexp = "^$|.*\\S.*", message = "vendor must not be blank")
    @Size(max = 100, message = "vendor must not exceed 100 characters")
    private String vendor;

    @Schema(description = "Whether the node is active.", example = "true")
    @NotNull(message = "active is required")
    private Boolean active;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
