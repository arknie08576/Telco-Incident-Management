package pl.telco.incident.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventFilterRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.dto.AlarmEventUpdateRequest;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.mapper.AlarmEventMapper;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
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
class AlarmEventServiceTest {

    @Mock
    private AlarmEventRepository alarmEventRepository;

    @Mock
    private NetworkNodeRepository networkNodeRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AlarmEventMapper alarmEventMapper;

    private AlarmEventService alarmEventService;
    private NetworkNode networkNode;

    @BeforeEach
    void setUp() {
        alarmEventService = new AlarmEventService(alarmEventRepository, networkNodeRepository, incidentRepository, alarmEventMapper);

        networkNode = NetworkNode.builder()
                .id(1L)
                .nodeName("CORE-RTR-WAW-01")
                .nodeType(NodeType.ROUTER)
                .region(Region.MAZOWIECKIE)
                .vendor("Cisco")
                .active(true)
                .build();

        lenient().when(alarmEventRepository.save(any(AlarmEvent.class))).thenAnswer(invocation -> {
            AlarmEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(100L);
            }
            if (event.getReceivedAt() == null) {
                event.setReceivedAt(LocalDateTime.now());
            }
            return event;
        });

        lenient().when(alarmEventMapper.toResponse(any(AlarmEvent.class))).thenAnswer(invocation -> {
            AlarmEvent event = invocation.getArgument(0);
            AlarmEventResponse response = new AlarmEventResponse();
            response.setId(event.getId());
            response.setSourceSystem(event.getSourceSystem());
            response.setExternalId(event.getExternalId());
            response.setNetworkNodeId(event.getNetworkNode() != null ? event.getNetworkNode().getId() : null);
            response.setIncidentId(event.getIncident() != null ? event.getIncident().getId() : null);
            response.setAlarmType(event.getAlarmType());
            response.setSeverity(event.getSeverity());
            response.setStatus(event.getStatus());
            response.setDescription(event.getDescription());
            response.setSuppressedByMaintenance(event.getSuppressedByMaintenance());
            response.setOccurredAt(event.getOccurredAt());
            response.setReceivedAt(event.getReceivedAt());
            return response;
        });
    }

    @Test
    void createAlarmEventShouldPersistAndReturnResponse() {
        AlarmEventCreateRequest request = buildCreateRequest();
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(networkNode));
        when(alarmEventRepository.existsBySourceSystemAndExternalId("NMS", "ALARM-001")).thenReturn(false);

        AlarmEventResponse response = alarmEventService.createAlarmEvent(request);

        ArgumentCaptor<AlarmEvent> captor = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarmEventRepository).save(captor.capture());

        assertThat(captor.getValue().getSourceSystem()).isEqualTo("NMS");
        assertThat(captor.getValue().getExternalId()).isEqualTo("ALARM-001");
        assertThat(captor.getValue().getNetworkNode()).isEqualTo(networkNode);
        assertThat(captor.getValue().getSuppressedByMaintenance()).isFalse();
        assertThat(response.getSourceSystem()).isEqualTo("NMS");
        assertThat(response.getNetworkNodeId()).isEqualTo(1L);
    }

    @Test
    void createAlarmEventShouldRejectDuplicateSourceSystemAndExternalId() {
        AlarmEventCreateRequest request = buildCreateRequest();
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(networkNode));
        when(alarmEventRepository.existsBySourceSystemAndExternalId("NMS", "ALARM-001")).thenReturn(true);

        assertThatThrownBy(() -> alarmEventService.createAlarmEvent(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Alarm event with sourceSystem and externalId already exists");

        verify(alarmEventRepository, never()).save(any(AlarmEvent.class));
    }

    @Test
    void createAlarmEventShouldRejectWhenNetworkNodeNotFound() {
        AlarmEventCreateRequest request = buildCreateRequest();
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmEventService.createAlarmEvent(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Network node not found: 1");
    }

    @Test
    void createAlarmEventShouldRejectWhenIncidentNotFound() {
        AlarmEventCreateRequest request = buildCreateRequest();
        request.setIncidentId(42L);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(networkNode));
        when(incidentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmEventService.createAlarmEvent(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Incident not found: 42");
    }

    @Test
    void createAlarmEventShouldDefaultSuppressedByMaintenanceToFalse() {
        AlarmEventCreateRequest request = buildCreateRequest();
        request.setSuppressedByMaintenance(null);
        when(networkNodeRepository.findById(1L)).thenReturn(Optional.of(networkNode));
        when(alarmEventRepository.existsBySourceSystemAndExternalId("NMS", "ALARM-001")).thenReturn(false);

        alarmEventService.createAlarmEvent(request);

        ArgumentCaptor<AlarmEvent> captor = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarmEventRepository).save(captor.capture());

        assertThat(captor.getValue().getSuppressedByMaintenance()).isFalse();
    }

    @Test
    void updateAlarmEventShouldApplyChanges() {
        AlarmEvent existing = buildAlarmEvent(1L, "NMS", "ALARM-001", AlarmSeverity.MAJOR, AlarmStatus.OPEN);
        when(alarmEventRepository.findById(1L)).thenReturn(Optional.of(existing));

        AlarmEventUpdateRequest request = new AlarmEventUpdateRequest();
        request.setSeverity(AlarmSeverity.CRITICAL);
        request.setStatus(AlarmStatus.ACKNOWLEDGED);

        AlarmEventResponse response = alarmEventService.updateAlarmEvent(1L, request);

        assertThat(existing.getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
        assertThat(existing.getStatus()).isEqualTo(AlarmStatus.ACKNOWLEDGED);
        assertThat(response.getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
        verify(alarmEventRepository).save(existing);
    }

    @Test
    void updateAlarmEventShouldRejectNoOpPatch() {
        AlarmEvent existing = buildAlarmEvent(1L, "NMS", "ALARM-001", AlarmSeverity.MAJOR, AlarmStatus.OPEN);
        when(alarmEventRepository.findById(1L)).thenReturn(Optional.of(existing));

        AlarmEventUpdateRequest request = new AlarmEventUpdateRequest();
        request.setSeverity(AlarmSeverity.MAJOR);
        request.setStatus(AlarmStatus.OPEN);

        assertThatThrownBy(() -> alarmEventService.updateAlarmEvent(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Patch request does not change alarm event");

        verify(alarmEventRepository, never()).save(any(AlarmEvent.class));
    }

    @Test
    void updateAlarmEventShouldThrowWhenNotFound() {
        when(alarmEventRepository.findById(999L)).thenReturn(Optional.empty());

        AlarmEventUpdateRequest request = new AlarmEventUpdateRequest();
        request.setSeverity(AlarmSeverity.CRITICAL);

        assertThatThrownBy(() -> alarmEventService.updateAlarmEvent(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alarm event not found: 999");
    }

    @Test
    void updateAlarmEventShouldCorrelateWithIncident() {
        AlarmEvent existing = buildAlarmEvent(1L, "NMS", "ALARM-001", AlarmSeverity.MAJOR, AlarmStatus.OPEN);
        Incident incident = Incident.builder().id(50L).build();
        when(alarmEventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(incidentRepository.findById(50L)).thenReturn(Optional.of(incident));

        AlarmEventUpdateRequest request = new AlarmEventUpdateRequest();
        request.setIncidentId(50L);

        AlarmEventResponse response = alarmEventService.updateAlarmEvent(1L, request);

        assertThat(existing.getIncident()).isEqualTo(incident);
        assertThat(response.getIncidentId()).isEqualTo(50L);
    }

    @Test
    void getAlarmEventByIdShouldReturnResponse() {
        AlarmEvent event = buildAlarmEvent(1L, "NMS", "ALARM-001", AlarmSeverity.MAJOR, AlarmStatus.OPEN);
        when(alarmEventRepository.findById(1L)).thenReturn(Optional.of(event));

        AlarmEventResponse response = alarmEventService.getAlarmEventById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSourceSystem()).isEqualTo("NMS");
    }

    @Test
    void getAlarmEventByIdShouldThrowWhenNotFound() {
        when(alarmEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmEventService.getAlarmEventById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Alarm event not found: 999");
    }

    @Test
    void getAlarmEventsShouldRejectUnsupportedSortBy() {
        AlarmEventFilterRequest filter = new AlarmEventFilterRequest();
        filter.setSortBy("invalidField");

        assertThatThrownBy(() -> alarmEventService.getAlarmEvents(filter))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sortBy value: invalidField");
    }

    @Test
    void getAlarmEventsShouldRejectInvalidSeverityFilter() {
        AlarmEventFilterRequest filter = new AlarmEventFilterRequest();
        filter.setSeverities(List.of("UNKNOWN_SEVERITY"));

        assertThatThrownBy(() -> alarmEventService.getAlarmEvents(filter))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid value 'UNKNOWN_SEVERITY' for parameter 'severities'");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAlarmEventsShouldReturnMappedPage() {
        AlarmEvent event = buildAlarmEvent(1L, "NMS", "ALARM-001", AlarmSeverity.CRITICAL, AlarmStatus.OPEN);
        when(alarmEventRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        AlarmEventFilterRequest filter = new AlarmEventFilterRequest();
        filter.setSortBy("severity");
        filter.setDirection("asc");

        Page<AlarmEventResponse> result = alarmEventService.getAlarmEvents(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
    }

    private AlarmEventCreateRequest buildCreateRequest() {
        AlarmEventCreateRequest request = new AlarmEventCreateRequest();
        request.setSourceSystem("NMS");
        request.setExternalId("ALARM-001");
        request.setNetworkNodeId(1L);
        request.setAlarmType("BGP_DOWN");
        request.setSeverity(AlarmSeverity.MAJOR);
        request.setStatus(AlarmStatus.OPEN);
        request.setOccurredAt(LocalDateTime.now().minusMinutes(10));
        return request;
    }

    private AlarmEvent buildAlarmEvent(Long id, String sourceSystem, String externalId, AlarmSeverity severity, AlarmStatus status) {
        AlarmEvent event = new AlarmEvent();
        event.setId(id);
        event.setSourceSystem(sourceSystem);
        event.setExternalId(externalId);
        event.setNetworkNode(networkNode);
        event.setAlarmType("BGP_DOWN");
        event.setSeverity(severity);
        event.setStatus(status);
        event.setSuppressedByMaintenance(false);
        event.setOccurredAt(LocalDateTime.now().minusMinutes(10));
        event.setReceivedAt(LocalDateTime.now());
        return event;
    }
}
