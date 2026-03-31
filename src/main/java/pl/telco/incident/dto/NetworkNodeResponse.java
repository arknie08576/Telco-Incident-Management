package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Schema(name = "NetworkNodeResponse", description = "Lookup data for an inventory network node.")
public class NetworkNodeResponse {

    @Schema(description = "Database identifier.", example = "1")
    private Long id;

    @Schema(description = "Unique node name.", example = "CORE-RTR-WAW-01")
    private String nodeName;

    @Schema(description = "Node type.", example = "ROUTER")
    private NodeType nodeType;

    @Schema(description = "Node region.", example = "MAZOWIECKIE")
    private Region region;

    @Schema(description = "Node vendor.", example = "Cisco", nullable = true)
    private String vendor;

    @Schema(description = "Whether the node is active.", example = "true")
    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
