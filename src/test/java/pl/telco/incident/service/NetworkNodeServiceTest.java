package pl.telco.incident.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.dto.NetworkNodeCreateRequest;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.dto.NetworkNodeUpdateRequest;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.mapper.NetworkNodeMapper;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkNodeServiceTest {

    @Mock
    private NetworkNodeRepository networkNodeRepository;

    @Mock
    private ObservabilityEventLogger observabilityEventLogger;

    @Mock
    private NetworkNodeMapper networkNodeMapper;

    private NetworkNodeService networkNodeService;

    @BeforeEach
    void setUp() {
        networkNodeService = new NetworkNodeService(networkNodeRepository, observabilityEventLogger, networkNodeMapper);

        lenient().when(networkNodeRepository.save(any(NetworkNode.class))).thenAnswer(invocation -> {
            NetworkNode node = invocation.getArgument(0);
            if (node.getId() == null) {
                node.setId(1L);
            }
            return node;
        });

        lenient().when(networkNodeMapper.toResponse(any(NetworkNode.class))).thenAnswer(invocation -> {
            NetworkNode node = invocation.getArgument(0);
            NetworkNodeResponse response = new NetworkNodeResponse();
            response.setId(node.getId());
            response.setNodeName(node.getNodeName());
            response.setNodeType(node.getNodeType());
            response.setRegion(node.getRegion());
            response.setVendor(node.getVendor());
            response.setActive(node.getActive());
            return response;
        });
    }

    @Test
    void createNetworkNodeShouldPersistAndReturnResponse() {
        NetworkNodeCreateRequest request = buildCreateRequest("CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.existsByNodeName("CORE-RTR-WAW-01")).thenReturn(false);

        NetworkNodeResponse response = networkNodeService.createNetworkNode(request);

        ArgumentCaptor<NetworkNode> captor = ArgumentCaptor.forClass(NetworkNode.class);
        verify(networkNodeRepository).save(captor.capture());

        assertThat(captor.getValue().getNodeName()).isEqualTo("CORE-RTR-WAW-01");
        assertThat(captor.getValue().getNodeType()).isEqualTo(NodeType.ROUTER);
        assertThat(captor.getValue().getRegion()).isEqualTo(Region.MAZOWIECKIE);
        assertThat(response.getNodeName()).isEqualTo("CORE-RTR-WAW-01");
    }

    @Test
    void createNetworkNodeShouldTrimNodeName() {
        NetworkNodeCreateRequest request = buildCreateRequest("  CORE-RTR-WAW-01  ", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.existsByNodeName("CORE-RTR-WAW-01")).thenReturn(false);

        networkNodeService.createNetworkNode(request);

        ArgumentCaptor<NetworkNode> captor = ArgumentCaptor.forClass(NetworkNode.class);
        verify(networkNodeRepository).save(captor.capture());

        assertThat(captor.getValue().getNodeName()).isEqualTo("CORE-RTR-WAW-01");
    }

    @Test
    void createNetworkNodeShouldRejectDuplicateName() {
        NetworkNodeCreateRequest request = buildCreateRequest("CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.existsByNodeName("CORE-RTR-WAW-01")).thenReturn(true);

        assertThatThrownBy(() -> networkNodeService.createNetworkNode(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Network node with name already exists: CORE-RTR-WAW-01");

        verify(networkNodeRepository, never()).save(any(NetworkNode.class));
    }

    @Test
    void updateNetworkNodeShouldApplyChanges() {
        NetworkNode existing = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(networkNodeRepository.findByNodeName("CORE-RTR-WAW-02")).thenReturn(Optional.empty());

        NetworkNodeUpdateRequest request = new NetworkNodeUpdateRequest();
        request.setNodeName("CORE-RTR-WAW-02");
        request.setNodeType(NodeType.SBC);

        NetworkNodeResponse response = networkNodeService.updateNetworkNode(1L, request);

        assertThat(existing.getNodeName()).isEqualTo("CORE-RTR-WAW-02");
        assertThat(existing.getNodeType()).isEqualTo(NodeType.SBC);
        assertThat(response.getNodeName()).isEqualTo("CORE-RTR-WAW-02");
        verify(networkNodeRepository).save(existing);
    }

    @Test
    void updateNetworkNodeShouldRejectNoOpPatch() {
        NetworkNode existing = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(networkNodeRepository.findByNodeName("CORE-RTR-WAW-01")).thenReturn(Optional.of(existing));

        NetworkNodeUpdateRequest request = new NetworkNodeUpdateRequest();
        request.setNodeName("CORE-RTR-WAW-01");
        request.setNodeType(NodeType.ROUTER);
        request.setRegion(Region.MAZOWIECKIE);

        assertThatThrownBy(() -> networkNodeService.updateNetworkNode(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Patch request does not change network node");

        verify(networkNodeRepository, never()).save(any(NetworkNode.class));
    }

    @Test
    void updateNetworkNodeShouldRejectDuplicateName() {
        NetworkNode existing = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        NetworkNode other = buildNode(2L, "CORE-RTR-WAW-02", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(networkNodeRepository.findByNodeName("CORE-RTR-WAW-02")).thenReturn(Optional.of(other));

        NetworkNodeUpdateRequest request = new NetworkNodeUpdateRequest();
        request.setNodeName("CORE-RTR-WAW-02");

        assertThatThrownBy(() -> networkNodeService.updateNetworkNode(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Network node with name already exists: CORE-RTR-WAW-02");
    }

    @Test
    void updateNetworkNodeShouldThrowWhenNotFound() {
        when(networkNodeRepository.findById(999L)).thenReturn(Optional.empty());

        NetworkNodeUpdateRequest request = new NetworkNodeUpdateRequest();
        request.setNodeName("anything");

        assertThatThrownBy(() -> networkNodeService.updateNetworkNode(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Network node not found: 999");
    }

    @Test
    void getNetworkNodeByIdShouldReturnResponse() {
        NetworkNode node = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(node));

        NetworkNodeResponse response = networkNodeService.getNetworkNodeById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNodeName()).isEqualTo("CORE-RTR-WAW-01");
    }

    @Test
    void getNetworkNodeByIdShouldThrowWhenNotFound() {
        when(networkNodeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> networkNodeService.getNetworkNodeById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Network node not found: 999");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getNetworkNodesShouldReturnMappedResults() {
        NetworkNode node = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        when(networkNodeRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(node));

        List<NetworkNodeResponse> result = networkNodeService.getNetworkNodes("CORE", Region.MAZOWIECKIE, NodeType.ROUTER, true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNodeName()).isEqualTo("CORE-RTR-WAW-01");
    }

    private NetworkNodeCreateRequest buildCreateRequest(String nodeName, NodeType nodeType, Region region) {
        NetworkNodeCreateRequest request = new NetworkNodeCreateRequest();
        request.setNodeName(nodeName);
        request.setNodeType(nodeType);
        request.setRegion(region);
        request.setVendor("TestVendor");
        request.setActive(true);
        return request;
    }

    private NetworkNode buildNode(Long id, String name, NodeType type, Region region) {
        return NetworkNode.builder()
                .id(id)
                .nodeName(name)
                .nodeType(type)
                .region(region)
                .vendor("TestVendor")
                .active(true)
                .build();
    }
}
