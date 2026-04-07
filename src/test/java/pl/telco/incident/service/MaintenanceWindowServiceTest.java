package pl.telco.incident.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowFilterRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.dto.MaintenanceWindowUpdateRequest;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.MaintenanceStatus;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.mapper.MaintenanceWindowMapper;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowServiceTest {

    @Mock
    private MaintenanceWindowRepository maintenanceWindowRepository;

    @Mock
    private NetworkNodeRepository networkNodeRepository;

    @Mock
    private MaintenanceWindowMapper maintenanceWindowMapper;

    private MaintenanceWindowService maintenanceWindowService;
    private NetworkNode nodeA;
    private NetworkNode nodeB;

    @BeforeEach
    void setUp() {
        maintenanceWindowService = new MaintenanceWindowService(maintenanceWindowRepository, networkNodeRepository, maintenanceWindowMapper);

        nodeA = buildNode(1L, "CORE-RTR-WAW-01");
        nodeB = buildNode(2L, "RAN-GNB-WAW-01");

        lenient().when(maintenanceWindowRepository.save(any(MaintenanceWindow.class))).thenAnswer(invocation -> {
            MaintenanceWindow window = invocation.getArgument(0);
            if (window.getId() == null) {
                window.setId(100L);
            }
            return window;
        });

        lenient().when(maintenanceWindowMapper.toResponse(any(MaintenanceWindow.class))).thenAnswer(invocation -> {
            MaintenanceWindow window = invocation.getArgument(0);
            MaintenanceWindowResponse response = new MaintenanceWindowResponse();
            response.setId(window.getId());
            response.setTitle(window.getTitle());
            response.setDescription(window.getDescription());
            response.setStatus(window.getStatus());
            response.setStartTime(window.getStartTime());
            response.setEndTime(window.getEndTime());
            response.setNodeIds(window.getMaintenanceNodes().stream()
                    .map(mn -> mn.getNetworkNode().getId())
                    .toList());
            return response;
        });
    }

    @Test
    void createMaintenanceWindowShouldPersistAndReturnResponse() {
        MaintenanceWindowCreateRequest request = buildCreateRequest(List.of(1L, 2L));
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(nodeA, nodeB));

        MaintenanceWindowResponse response = maintenanceWindowService.createMaintenanceWindow(request);

        ArgumentCaptor<MaintenanceWindow> captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
        verify(maintenanceWindowRepository).save(captor.capture());

        MaintenanceWindow saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Planned upgrade");
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
        assertThat(saved.getMaintenanceNodes()).hasSize(2);
        assertThat(response.getNodeIds()).containsExactly(1L, 2L);
    }

    @Test
    void createMaintenanceWindowShouldTrimTitle() {
        MaintenanceWindowCreateRequest request = buildCreateRequest(List.of(1L));
        request.setTitle("  Planned upgrade  ");
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(nodeA));

        maintenanceWindowService.createMaintenanceWindow(request);

        ArgumentCaptor<MaintenanceWindow> captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
        verify(maintenanceWindowRepository).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Planned upgrade");
    }

    @Test
    void createMaintenanceWindowShouldDeduplicateNodeIds() {
        MaintenanceWindowCreateRequest request = buildCreateRequest(List.of(1L, 1L, 2L));
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(nodeA, nodeB));

        maintenanceWindowService.createMaintenanceWindow(request);

        ArgumentCaptor<MaintenanceWindow> captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
        verify(maintenanceWindowRepository).save(captor.capture());

        assertThat(captor.getValue().getMaintenanceNodes()).hasSize(2);
    }

    @Test
    void createMaintenanceWindowShouldRejectWhenNodeNotFound() {
        MaintenanceWindowCreateRequest request = buildCreateRequest(List.of(1L, 999L));
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(nodeA));

        assertThatThrownBy(() -> maintenanceWindowService.createMaintenanceWindow(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("One or more network nodes were not found");

        verify(maintenanceWindowRepository, never()).save(any(MaintenanceWindow.class));
    }

    @Test
    void updateMaintenanceWindowShouldApplyChanges() {
        MaintenanceWindow existing = buildMaintenanceWindow(1L, "Old title", MaintenanceStatus.PLANNED);
        when(maintenanceWindowRepository.findById(1L)).thenReturn(Optional.of(existing));

        MaintenanceWindowUpdateRequest request = new MaintenanceWindowUpdateRequest();
        request.setTitle("New title");
        request.setStatus(MaintenanceStatus.IN_PROGRESS);

        MaintenanceWindowResponse response = maintenanceWindowService.updateMaintenanceWindow(1L, request);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(response.getTitle()).isEqualTo("New title");
        verify(maintenanceWindowRepository).save(existing);
    }

    @Test
    void updateMaintenanceWindowShouldRejectNoOpPatch() {
        MaintenanceWindow existing = buildMaintenanceWindow(1L, "Planned upgrade", MaintenanceStatus.PLANNED);
        when(maintenanceWindowRepository.findById(1L)).thenReturn(Optional.of(existing));

        MaintenanceWindowUpdateRequest request = new MaintenanceWindowUpdateRequest();
        request.setTitle("Planned upgrade");
        request.setStatus(MaintenanceStatus.PLANNED);

        assertThatThrownBy(() -> maintenanceWindowService.updateMaintenanceWindow(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Patch request does not change maintenance window");

        verify(maintenanceWindowRepository, never()).save(any(MaintenanceWindow.class));
    }

    @Test
    void updateMaintenanceWindowShouldRejectInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 0);
        MaintenanceWindow existing = buildMaintenanceWindow(1L, "Planned upgrade", MaintenanceStatus.PLANNED);
        existing.setStartTime(start);
        existing.setEndTime(end);
        when(maintenanceWindowRepository.findById(1L)).thenReturn(Optional.of(existing));

        MaintenanceWindowUpdateRequest request = new MaintenanceWindowUpdateRequest();
        request.setStartTime(end);
        request.setEndTime(start);

        assertThatThrownBy(() -> maintenanceWindowService.updateMaintenanceWindow(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("endTime must be later than startTime");
    }

    @Test
    void updateMaintenanceWindowShouldThrowWhenNotFound() {
        when(maintenanceWindowRepository.findById(999L)).thenReturn(Optional.empty());

        MaintenanceWindowUpdateRequest request = new MaintenanceWindowUpdateRequest();
        request.setTitle("anything");

        assertThatThrownBy(() -> maintenanceWindowService.updateMaintenanceWindow(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Maintenance window not found: 999");
    }

    @Test
    void updateMaintenanceWindowShouldSyncNodeIds() {
        MaintenanceWindow existing = buildMaintenanceWindow(1L, "Planned upgrade", MaintenanceStatus.PLANNED);
        MaintenanceNode existingNode = new MaintenanceNode();
        existingNode.setNetworkNode(nodeA);
        existing.addMaintenanceNode(existingNode);

        when(maintenanceWindowRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(nodeB));

        MaintenanceWindowUpdateRequest request = new MaintenanceWindowUpdateRequest();
        request.setNodeIds(List.of(2L));

        MaintenanceWindowResponse response = maintenanceWindowService.updateMaintenanceWindow(1L, request);

        assertThat(existing.getMaintenanceNodes()).hasSize(1);
        assertThat(existing.getMaintenanceNodes().getFirst().getNetworkNode().getId()).isEqualTo(2L);
        assertThat(response.getNodeIds()).containsExactly(2L);
    }

    @Test
    void getMaintenanceWindowByIdShouldReturnResponse() {
        MaintenanceWindow window = buildMaintenanceWindow(1L, "Planned upgrade", MaintenanceStatus.PLANNED);
        when(maintenanceWindowRepository.findById(1L)).thenReturn(Optional.of(window));

        MaintenanceWindowResponse response = maintenanceWindowService.getMaintenanceWindowById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Planned upgrade");
    }

    @Test
    void getMaintenanceWindowByIdShouldThrowWhenNotFound() {
        when(maintenanceWindowRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceWindowService.getMaintenanceWindowById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Maintenance window not found: 999");
    }

    @Test
    void getMaintenanceWindowsShouldRejectUnsupportedSortBy() {
        MaintenanceWindowFilterRequest filter = new MaintenanceWindowFilterRequest();
        filter.setSortBy("invalidField");

        assertThatThrownBy(() -> maintenanceWindowService.getMaintenanceWindows(filter))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sortBy value: invalidField");
    }

    @Test
    void getMaintenanceWindowsShouldRejectInvalidStatusFilter() {
        MaintenanceWindowFilterRequest filter = new MaintenanceWindowFilterRequest();
        filter.setStatuses(List.of("NONEXISTENT"));

        assertThatThrownBy(() -> maintenanceWindowService.getMaintenanceWindows(filter))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid value 'NONEXISTENT' for parameter 'statuses'");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMaintenanceWindowsShouldReturnMappedPage() {
        MaintenanceWindow window = buildMaintenanceWindow(1L, "Planned upgrade", MaintenanceStatus.PLANNED);
        when(maintenanceWindowRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(window)));

        MaintenanceWindowFilterRequest filter = new MaintenanceWindowFilterRequest();
        filter.setSortBy("title");
        filter.setDirection("asc");

        Page<MaintenanceWindowResponse> result = maintenanceWindowService.getMaintenanceWindows(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Planned upgrade");
    }

    private MaintenanceWindowCreateRequest buildCreateRequest(List<Long> nodeIds) {
        MaintenanceWindowCreateRequest request = new MaintenanceWindowCreateRequest();
        request.setTitle("Planned upgrade");
        request.setDescription("Firmware upgrade on core routers");
        request.setStatus(MaintenanceStatus.PLANNED);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(4));
        request.setNodeIds(nodeIds);
        return request;
    }

    private MaintenanceWindow buildMaintenanceWindow(Long id, String title, MaintenanceStatus status) {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId(id);
        window.setTitle(title);
        window.setDescription("Test description");
        window.setStatus(status);
        window.setStartTime(LocalDateTime.of(2026, 6, 1, 8, 0));
        window.setEndTime(LocalDateTime.of(2026, 6, 1, 12, 0));
        return window;
    }

    private NetworkNode buildNode(Long id, String name) {
        return NetworkNode.builder()
                .id(id)
                .nodeName(name)
                .nodeType(NodeType.ROUTER)
                .region(Region.MAZOWIECKIE)
                .vendor("Cisco")
                .active(true)
                .build();
    }
}
