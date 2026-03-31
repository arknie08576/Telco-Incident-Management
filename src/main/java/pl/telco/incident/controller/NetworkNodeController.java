package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.service.NetworkNodeService;

import java.util.List;

@RestController
@RequestMapping("/api/network-nodes")
@RequiredArgsConstructor
@Tag(name = "Network Nodes")
public class NetworkNodeController {

    private final NetworkNodeService networkNodeService;

    @GetMapping
    @Operation(
            summary = "List network nodes",
            description = "Returns inventory nodes that can be used for incident creation or lookup UIs."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Network node list returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NetworkNodeResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter values",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public List<NetworkNodeResponse> getNetworkNodes(
            @Parameter(description = "Case-insensitive partial match on node name", example = "WAW")
            @RequestParam(name = "q", required = false) String query,
            @Parameter(description = "Region filter", example = "MAZOWIECKIE")
            @RequestParam(name = "region", required = false) Region region,
            @Parameter(description = "Node type filter", example = "ROUTER")
            @RequestParam(name = "nodeType", required = false) NodeType nodeType,
            @Parameter(description = "Active flag filter", example = "true")
            @RequestParam(name = "active", required = false) Boolean active
    ) {
        return networkNodeService.getNetworkNodes(query, region, nodeType, active);
    }
}
