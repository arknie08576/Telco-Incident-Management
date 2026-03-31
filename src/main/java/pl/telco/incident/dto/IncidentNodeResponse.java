package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Schema(name = "IncidentNodeResponse", description = "Network node linked to an incident.")
public class IncidentNodeResponse {

    @Schema(description = "Identifier of the linked network node.", example = "10")
    private Long networkNodeId;

    @Schema(description = "Inventory node name.", example = "CORE-RTR-WAW-01")
    private String nodeName;

    @Schema(description = "Role of the node in the incident graph.", example = "ROOT")
    private IncidentNodeRole role;

    @Schema(description = "Node type.", example = "ROUTER")
    private NodeType nodeType;

    @Schema(description = "Node region.", example = "MAZOWIECKIE")
    private Region region;

    @Schema(description = "Node vendor.", example = "Cisco", nullable = true)
    private String vendor;

    @Schema(description = "Whether the node is active.", example = "true")
    private Boolean active;

    public Long getNetworkNodeId() {
        return networkNodeId;
    }

    public void setNetworkNodeId(Long networkNodeId) {
        this.networkNodeId = networkNodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public IncidentNodeRole getRole() {
        return role;
    }

    public void setRole(IncidentNodeRole role) {
        this.role = role;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
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
