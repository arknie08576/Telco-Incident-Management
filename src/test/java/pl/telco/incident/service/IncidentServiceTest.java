package pl.telco.incident.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentNodeRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentSummaryResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentUpdateRequest;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
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
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NetworkNodeRepository networkNodeRepository;

    @Mock
    private IncidentTimelineRepository incidentTimelineRepository;

    private IncidentService incidentService;
    private SimpleMeterRegistry meterRegistry;
    private NetworkNode rootNode;
    private NetworkNode affectedNode;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        incidentService = new IncidentService(incidentRepository, networkNodeRepository, incidentTimelineRepository, meterRegistry);

        rootNode = buildNode(1L, "CORE-RTR-WAW-01", NodeType.ROUTER, Region.MAZOWIECKIE);
        affectedNode = buildNode(2L, "RAN-GNB-WAW-01", NodeType.G_NODE_B, Region.MAZOWIECKIE);

        lenient().when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);

            if (incident.getId() == null) {
                incident.setId(100L);
            }
            if (incident.getStatus() == null) {
                incident.setStatus(IncidentStatus.OPEN);
            }
            if (incident.getPossiblyPlanned() == null) {
                incident.setPossiblyPlanned(false);
            }
            if (incident.getOpenedAt() == null) {
                incident.setOpenedAt(LocalDateTime.now());
            }
            if (incident.getCreatedAt() == null) {
                incident.setCreatedAt(LocalDateTime.now());
            }
            if (incident.getVersion() == null) {
                incident.setVersion(0L);
            }
            incident.setUpdatedAt(LocalDateTime.now());

            return incident;
        });

        lenient().when(incidentTimelineRepository.save(any(IncidentTimeline.class))).thenAnswer(invocation -> {
            IncidentTimeline timeline = invocation.getArgument(0);
            timeline.setCreatedAt(timeline.getCreatedAt() != null ? timeline.getCreatedAt() : LocalDateTime.now());
            return timeline;
        });
    }

    @Test
    void createIncidentShouldPersistIncidentAndCreatedTimelineEvent() {
        IncidentCreateRequest request = buildCreateRequest("INC-100", rootNode.getId(), List.of(
                buildNodeRequest(rootNode.getId(), IncidentNodeRole.ROOT),
                buildNodeRequest(affectedNode.getId(), IncidentNodeRole.AFFECTED)
        ));

        when(incidentRepository.findByIncidentNumber("INC-100")).thenReturn(Optional.empty());
        when(networkNodeRepository.findAllById(any())).thenReturn(List.of(rootNode, affectedNode));

        IncidentResponse response = incidentService.createIncident(request);

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        ArgumentCaptor<IncidentTimeline> timelineCaptor = ArgumentCaptor.forClass(IncidentTimeline.class);

        verify(incidentRepository).save(incidentCaptor.capture());
        verify(incidentTimelineRepository).save(timelineCaptor.capture());

        Incident savedIncident = incidentCaptor.getValue();
        IncidentTimeline timeline = timelineCaptor.getValue();

        assertThat(response.getIncidentNumber()).isEqualTo("INC-100");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.getRootNodeId()).isEqualTo(rootNode.getId());
        assertThat(response.getNodes()).hasSize(2);
        assertThat(savedIncident.getRootNode()).isEqualTo(rootNode);
        assertThat(savedIncident.getIncidentNodes()).hasSize(2);
        assertThat(savedIncident.getIncidentNodes())
                .extracting(node -> node.getNetworkNode().getId())
                .containsExactly(rootNode.getId(), affectedNode.getId());
        assertThat(timeline.getEventType()).isEqualTo(IncidentTimelineEventType.CREATED);
        assertThat(timeline.getMessage()).isEqualTo("Incident created");
        assertThat(meterRegistry.find("incident.created").counter()).isNotNull();
    }

    @Test
    void createIncidentShouldRejectDuplicateNetworkNodes() {
        IncidentCreateRequest request = buildCreateRequest("INC-101", rootNode.getId(), List.of(
                buildNodeRequest(rootNode.getId(), IncidentNodeRole.ROOT),
                buildNodeRequest(rootNode.getId(), IncidentNodeRole.AFFECTED)
        ));

        when(incidentRepository.findByIncidentNumber("INC-101")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Duplicate networkNodeId in nodes: 1");

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void createIncidentShouldRejectWhenRootNodeDoesNotMatchRootRole() {
        IncidentCreateRequest request = buildCreateRequest("INC-102", affectedNode.getId(), List.of(
                buildNodeRequest(rootNode.getId(), IncidentNodeRole.ROOT),
                buildNodeRequest(affectedNode.getId(), IncidentNodeRole.AFFECTED)
        ));

        when(incidentRepository.findByIncidentNumber("INC-102")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("rootNodeId must match the node with role ROOT");
    }

    @Test
    void createIncidentShouldRejectDuplicateIncidentNumber() {
        IncidentCreateRequest request = buildCreateRequest("INC-103", rootNode.getId(), List.of(
                buildNodeRequest(rootNode.getId(), IncidentNodeRole.ROOT)
        ));

        when(incidentRepository.findByIncidentNumber("INC-103"))
                .thenReturn(Optional.of(Incident.builder().id(55L).incidentNumber("INC-103").build()));

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Incident with number already exists: INC-103");
    }

    @Test
    void updateIncidentShouldApplyChangedFieldsAndAddUpdatedTimelineEvent() {
        Incident incident = buildIncident(150L, "INC-150", IncidentStatus.OPEN);
        when(incidentRepository.findById(150L)).thenReturn(Optional.of(incident));
        when(incidentRepository.findByIncidentNumber("INC-151")).thenReturn(Optional.empty());

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setIncidentNumber("INC-151");
        request.setTitle("Updated incident");
        request.setPriority(IncidentPriority.CRITICAL);
        request.setRegion(Region.SLASKIE);
        request.setSourceAlarmType(SourceAlarmType.POWER);
        request.setPossiblyPlanned(true);

        IncidentResponse response = incidentService.updateIncident(150L, request);

        ArgumentCaptor<IncidentTimeline> timelineCaptor = ArgumentCaptor.forClass(IncidentTimeline.class);
        verify(incidentTimelineRepository).save(timelineCaptor.capture());

        assertThat(response.getIncidentNumber()).isEqualTo("INC-151");
        assertThat(response.getTitle()).isEqualTo("Updated incident");
        assertThat(response.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(response.getRegion()).isEqualTo(Region.SLASKIE);
        assertThat(incident.getSourceAlarmType()).isEqualTo(SourceAlarmType.POWER);
        assertThat(incident.getPossiblyPlanned()).isTrue();
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(IncidentTimelineEventType.UPDATED);
        assertThat(timelineCaptor.getValue().getMessage())
                .isEqualTo("Incident updated: incidentNumber, title, priority, region, sourceAlarmType, possiblyPlanned");
        assertThat(meterRegistry.find("incident.updated").counter()).isNotNull();
    }

    @Test
    void updateIncidentShouldRejectNoOpPatch() {
        Incident incident = buildIncident(151L, "INC-151", IncidentStatus.OPEN);
        when(incidentRepository.findById(151L)).thenReturn(Optional.of(incident));

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setIncidentNumber("INC-151");
        request.setTitle("Test incident");
        request.setPriority(IncidentPriority.HIGH);
        request.setRegion(Region.MAZOWIECKIE);
        request.setSourceAlarmType(SourceAlarmType.HARDWARE);
        request.setPossiblyPlanned(false);

        assertThatThrownBy(() -> incidentService.updateIncident(151L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Patch request does not change incident");

        verify(incidentRepository, never()).save(any(Incident.class));
        verify(incidentTimelineRepository, never()).save(any(IncidentTimeline.class));
    }

    @Test
    void updateIncidentShouldRejectClosedIncident() {
        Incident incident = buildIncident(152L, "INC-152", IncidentStatus.CLOSED);
        when(incidentRepository.findById(152L)).thenReturn(Optional.of(incident));

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setTitle("Should fail");

        assertThatThrownBy(() -> incidentService.updateIncident(152L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Closed incidents cannot be edited");

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void updateIncidentShouldRejectDuplicateIncidentNumber() {
        Incident incident = buildIncident(153L, "INC-153", IncidentStatus.OPEN);
        when(incidentRepository.findById(153L)).thenReturn(Optional.of(incident));
        when(incidentRepository.findByIncidentNumber("INC-154"))
                .thenReturn(Optional.of(Incident.builder().id(154L).incidentNumber("INC-154").build()));

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setIncidentNumber("INC-154");

        assertThatThrownBy(() -> incidentService.updateIncident(153L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Incident with number already exists: INC-154");

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void acknowledgeIncidentShouldUpdateStatusTimestampAndDefaultTimelineMessage() {
        Incident incident = buildIncident(200L, "INC-200", IncidentStatus.OPEN);
        when(incidentRepository.findById(200L)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.acknowledgeIncident(200L, null);

        ArgumentCaptor<IncidentTimeline> timelineCaptor = ArgumentCaptor.forClass(IncidentTimeline.class);
        verify(incidentTimelineRepository).save(timelineCaptor.capture());

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
        assertThat(incident.getAcknowledgedAt()).isNotNull();
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(IncidentTimelineEventType.ACKNOWLEDGED);
        assertThat(timelineCaptor.getValue().getMessage()).isEqualTo("Incident acknowledged");
        assertThat(meterRegistry.find("incident.lifecycle.transition").counter()).isNotNull();
        assertThat(meterRegistry.find("incident.time.to_ack").timer()).isNotNull();
    }

    @Test
    void resolveIncidentShouldAppendActionNoteToTimelineMessage() {
        Incident incident = buildIncident(201L, "INC-201", IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now().minusHours(1));
        when(incidentRepository.findById(201L)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.resolveIncident(201L, new IncidentActionRequest("Traffic rerouted"));

        ArgumentCaptor<IncidentTimeline> timelineCaptor = ArgumentCaptor.forClass(IncidentTimeline.class);
        verify(incidentTimelineRepository).save(timelineCaptor.capture());

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.getResolvedAt()).isNotNull();
        assertThat(timelineCaptor.getValue().getEventType()).isEqualTo(IncidentTimelineEventType.RESOLVED);
        assertThat(timelineCaptor.getValue().getMessage()).isEqualTo("Incident resolved: Traffic rerouted");
        assertThat(meterRegistry.find("incident.time.to_resolve").timer()).isNotNull();
    }

    @Test
    void closeIncidentShouldRejectInvalidTransition() {
        Incident incident = buildIncident(202L, "INC-202", IncidentStatus.OPEN);
        when(incidentRepository.findById(202L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.closeIncident(202L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only RESOLVED incidents can be closed");

        verify(incidentTimelineRepository, never()).save(any(IncidentTimeline.class));
    }

    @Test
    void getAllIncidentsShouldRejectUnsupportedSortByField() {
        assertThatThrownBy(() -> incidentService.getAllIncidents(
                0,
                10,
                "createdAt",
                "asc",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sortBy value: createdAt");
    }

    @Test
    void getAllIncidentsShouldRejectOpenedAtRangeWhenFromIsAfterTo() {
        assertThatThrownBy(() -> incidentService.getAllIncidents(
                0,
                10,
                "openedAt",
                "desc",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                null,
                null,
                null,
                null,
                null,
                null
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("openedFrom must be earlier than or equal to openedTo");
    }

    @Test
    void getAllIncidentsShouldRejectAcknowledgedAtRangeWhenFromIsAfterTo() {
        assertThatThrownBy(() -> incidentService.getAllIncidents(
                0,
                10,
                "openedAt",
                "desc",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 30, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                null,
                null,
                null,
                null
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("acknowledgedFrom must be earlier than or equal to acknowledgedTo");
    }

    @Test
    void getAllIncidentsShouldMapRepositoryPage() {
        Incident incident = buildIncident(203L, "INC-203", IncidentStatus.OPEN);
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setRegion(Region.POMORSKIE);

        when(incidentRepository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Incident>>any(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incident)));

        Page<IncidentSummaryResponse> result = incidentService.getAllIncidents(
                0,
                10,
                "incidentNumber",
                "asc",
                IncidentPriority.CRITICAL,
                List.of("HIGH", "CRITICAL"),
                Region.POMORSKIE,
                false,
                IncidentStatus.OPEN,
                List.of("OPEN", "RESOLVED"),
                "INC-203",
                "test",
                null,
                LocalDateTime.of(2026, 3, 29, 0, 0),
                LocalDateTime.of(2026, 3, 29, 23, 59),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIncidentNumber()).isEqualTo("INC-203");
        assertThat(result.getContent().getFirst().getPriority()).isEqualTo(IncidentPriority.CRITICAL);
    }

    @Test
    void getAllIncidentsShouldRejectInvalidMultiValuePriorityFilter() {
        assertThatThrownBy(() -> incidentService.getAllIncidents(
                0,
                10,
                "openedAt",
                "desc",
                null,
                List.of("URGENT"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid value 'URGENT' for parameter 'priorities'");
    }

    @Test
    void getIncidentTimelineShouldMapRepositoryEntries() {
        Incident incident = buildIncident(204L, "INC-204", IncidentStatus.RESOLVED);
        IncidentTimeline created = buildTimeline(incident, IncidentTimelineEventType.CREATED, "Incident created", LocalDateTime.now().minusHours(2));
        IncidentTimeline resolved = buildTimeline(incident, IncidentTimelineEventType.RESOLVED, "Incident resolved", LocalDateTime.now().minusHours(1));

        when(incidentRepository.findById(204L)).thenReturn(Optional.of(incident));
        when(incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(204L)).thenReturn(List.of(created, resolved));

        List<IncidentTimelineResponse> result = incidentService.getIncidentTimeline(204L);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getEventType()).isEqualTo(IncidentTimelineEventType.CREATED);
        assertThat(result.get(1).getEventType()).isEqualTo(IncidentTimelineEventType.RESOLVED);
    }

    private IncidentCreateRequest buildCreateRequest(String incidentNumber, Long rootNodeId, List<IncidentNodeRequest> nodes) {
        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setIncidentNumber(incidentNumber);
        request.setTitle("Test incident");
        request.setPriority(IncidentPriority.HIGH);
        request.setRegion(Region.MAZOWIECKIE);
        request.setSourceAlarmType(SourceAlarmType.HARDWARE);
        request.setPossiblyPlanned(false);
        request.setRootNodeId(rootNodeId);
        request.setNodes(nodes);
        return request;
    }

    private IncidentNodeRequest buildNodeRequest(Long nodeId, IncidentNodeRole role) {
        IncidentNodeRequest request = new IncidentNodeRequest();
        request.setNetworkNodeId(nodeId);
        request.setRole(role);
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

    private Incident buildIncident(Long id, String incidentNumber, IncidentStatus status) {
        Incident incident = Incident.builder()
                .id(id)
                .incidentNumber(incidentNumber)
                .title("Test incident")
                .status(status)
                .priority(IncidentPriority.HIGH)
                .region(Region.MAZOWIECKIE)
                .sourceAlarmType(SourceAlarmType.HARDWARE)
                .possiblyPlanned(false)
                .rootNode(rootNode)
                .openedAt(LocalDateTime.now().minusHours(3))
                .build();

        IncidentNode rootIncidentNode = new IncidentNode();
        rootIncidentNode.setNetworkNode(rootNode);
        rootIncidentNode.setRole(IncidentNodeRole.ROOT);
        incident.addIncidentNode(rootIncidentNode);

        return incident;
    }

    private IncidentTimeline buildTimeline(
            Incident incident,
            IncidentTimelineEventType eventType,
            String message,
            LocalDateTime createdAt
    ) {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(eventType);
        timeline.setMessage(message);
        timeline.setCreatedAt(createdAt);
        return timeline;
    }
}
