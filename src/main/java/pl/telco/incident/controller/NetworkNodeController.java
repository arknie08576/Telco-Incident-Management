package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.NetworkNodeRequest;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.service.NetworkNodeService;

import java.util.List;

@RestController
@RequestMapping("/api/network-nodes")
@RequiredArgsConstructor
@Tag(name = "Network Nodes")
public class NetworkNodeController {

    private final NetworkNodeService networkNodeService;

    @GetMapping
    @Operation(summary = "List network nodes")
    public List<NetworkNodeResponse> getAllNetworkNodes() {
        return networkNodeService.getAllNetworkNodes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get network node by ID")
    public NetworkNodeResponse getNetworkNodeById(@PathVariable("id") Long id) {
        return networkNodeService.getNetworkNodeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create network node")
    public NetworkNodeResponse createNetworkNode(@Valid @RequestBody NetworkNodeRequest request) {
        return networkNodeService.createNetworkNode(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update network node")
    public NetworkNodeResponse updateNetworkNode(
            @PathVariable("id") Long id,
            @Valid @RequestBody NetworkNodeRequest request
    ) {
        return networkNodeService.updateNetworkNode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete network node")
    public void deleteNetworkNode(@PathVariable("id") Long id) {
        networkNodeService.deleteNetworkNode(id);
    }
}
