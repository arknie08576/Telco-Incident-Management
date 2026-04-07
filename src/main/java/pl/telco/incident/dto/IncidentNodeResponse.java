package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Data
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
}
