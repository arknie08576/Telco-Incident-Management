package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

public class NetworkNodeUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "nodeName must not be blank")
    @Size(max = 100, message = "nodeName must not exceed 100 characters")
    private String nodeName;

    private NodeType nodeType;

    private Region region;

    @Pattern(regexp = ".*\\S.*", message = "vendor must not be blank")
    @Size(max = 100, message = "vendor must not exceed 100 characters")
    private String vendor;

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
