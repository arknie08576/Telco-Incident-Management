package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Data
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
}
