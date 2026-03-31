package pl.telco.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkNodeRepository networkNodeRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentTimelineRepository incidentTimelineRepository;

    @BeforeEach
    void cleanDatabase() {
        incidentTimelineRepository.deleteAllInBatch();
        incidentRepository.deleteAllInBatch();
        networkNodeRepository.deleteAllInBatch();
    }

    @Test
    void createIncidentShouldPersistAndAddCreatedTimelineEvent() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-100",
                  "title": "Router failure in Warsaw",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "sourceAlarmType": "HARDWARE",
                  "possiblyPlanned": false,
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(rootNode.getId(), rootNode.getId(), affectedNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentNumber").value("INC-100"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.openedAt").exists());

        Incident savedIncident = incidentRepository.findByIncidentNumber("INC-100").orElseThrow();
        List<IncidentTimeline> timeline = incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(savedIncident.getId());

        assertThat(savedIncident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(savedIncident.getOpenedAt()).isNotNull();
        assertThat(timeline).hasSize(1);
        assertThat(timeline.getFirst().getEventType()).isEqualTo(IncidentTimelineEventType.CREATED);
        assertThat(timeline.getFirst().getMessage()).isEqualTo("Incident created");
    }

    @Test
    void getIncidentByIdShouldReturnDetailedResponseWithNodes() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-11", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-11", NodeType.G_NODE_B, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-DETAIL-1",
                  "title": "Detailed incident",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "sourceAlarmType": "HARDWARE",
                  "possiblyPlanned": false,
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(rootNode.getId(), rootNode.getId(), affectedNode.getId());

        String responseBody = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long incidentId = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(get("/api/incidents/{id}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootNodeId").value(rootNode.getId()))
                .andExpect(jsonPath("$.sourceAlarmType").value("HARDWARE"))
                .andExpect(jsonPath("$.possiblyPlanned").value(false))
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].networkNodeId").value(rootNode.getId()))
                .andExpect(jsonPath("$.nodes[0].role").value("ROOT"));
    }

    @Test
    void createIncidentShouldReturnBadRequestWhenNodesContainDuplicates() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-101",
                  "title": "Duplicate node validation",
                  "priority": "MEDIUM",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(rootNode.getId(), rootNode.getId(), rootNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate networkNodeId in nodes: " + rootNode.getId()));
    }

    @Test
    void createIncidentShouldReturnConflictForDuplicateIncidentNumber() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");
        saveIncident("INC-102", "Existing incident", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);

        String requestBody = """
                {
                  "incidentNumber": "INC-102",
                  "title": "Duplicate number",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(rootNode.getId(), rootNode.getId(), affectedNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incident with number already exists: INC-102"));
    }

    @Test
    void createIncidentShouldReturnBadRequestWhenRootNodeIdDoesNotMatchRootRole() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-102A",
                  "title": "Invalid root mapping",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(affectedNode.getId(), rootNode.getId(), affectedNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("rootNodeId must match the node with role ROOT"));
    }

    @Test
    void createIncidentShouldReturnNotFoundWhenRootNodeDoesNotExist() throws Exception {
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-102B",
                  "title": "Missing root node",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": 9999,
                  "nodes": [
                    {
                      "networkNodeId": 9999,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(affectedNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Root node not found: 9999"));
    }

    @Test
    void createIncidentShouldReturnNotFoundWhenAffectedNodeDoesNotExist() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");

        String requestBody = """
                {
                  "incidentNumber": "INC-102C",
                  "title": "Missing affected node",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": 9998,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(rootNode.getId(), rootNode.getId());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Network node not found: 9998"));
    }

    @Test
    void updateIncidentShouldPatchEditableFieldsAndAddTimelineEvent() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-GDN-01", NodeType.ROUTER, "POMORSKIE");
        Incident incident = saveIncident("INC-102D", "Original incident", IncidentStatus.OPEN, IncidentPriority.HIGH, "POMORSKIE", rootNode);

        mockMvc.perform(patch("/api/incidents/{id}", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-102E",
                                  "title": "Updated incident title",
                                  "priority": "CRITICAL",
                                  "region": "SLASKIE",
                                  "sourceAlarmType": "POWER",
                                  "possiblyPlanned": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentNumber").value("INC-102E"))
                .andExpect(jsonPath("$.title").value("Updated incident title"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.region").value("SLASKIE"));

        Incident updatedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        List<IncidentTimeline> timeline = incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

        assertThat(updatedIncident.getIncidentNumber()).isEqualTo("INC-102E");
        assertThat(updatedIncident.getTitle()).isEqualTo("Updated incident title");
        assertThat(updatedIncident.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(updatedIncident.getRegion()).isEqualTo("SLASKIE");
        assertThat(updatedIncident.getSourceAlarmType()).isEqualTo("POWER");
        assertThat(updatedIncident.getPossiblyPlanned()).isTrue();
        assertThat(timeline).hasSize(1);
        assertThat(timeline.getFirst().getEventType()).isEqualTo(IncidentTimelineEventType.UPDATED);
        assertThat(timeline.getFirst().getMessage())
                .isEqualTo("Incident updated: incidentNumber, title, priority, region, sourceAlarmType, possiblyPlanned");
    }

    @Test
    void updateIncidentShouldReturnConflictForDuplicateIncidentNumber() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-LOD-01", NodeType.ROUTER, "LODZKIE");
        saveIncident("INC-110", "Existing incident", IncidentStatus.OPEN, IncidentPriority.HIGH, "LODZKIE", rootNode);
        Incident incidentToUpdate = saveIncident("INC-111", "Editable incident", IncidentStatus.OPEN, IncidentPriority.MEDIUM, "LODZKIE", rootNode);

        mockMvc.perform(patch("/api/incidents/{id}", incidentToUpdate.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-110"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incident with number already exists: INC-110"));
    }

    @Test
    void updateIncidentShouldReturnBadRequestForNoOpPatch() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-SZC-01", NodeType.ROUTER, "ZACHODNIOPOMORSKIE");
        Incident incident = saveIncident("INC-112", "No-op incident", IncidentStatus.OPEN, IncidentPriority.HIGH, "ZACHODNIOPOMORSKIE", rootNode);

        mockMvc.perform(patch("/api/incidents/{id}", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-112",
                                  "title": "No-op incident",
                                  "priority": "HIGH",
                                  "region": "ZACHODNIOPOMORSKIE",
                                  "sourceAlarmType": "HARDWARE",
                                  "possiblyPlanned": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patch request does not change incident"));
    }

    @Test
    void updateIncidentShouldRejectClosedIncident() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WRO-01", NodeType.ROUTER, "DOLNOSLASKIE");
        Incident incident = saveIncident("INC-113", "Closed incident", IncidentStatus.CLOSED, IncidentPriority.HIGH, "DOLNOSLASKIE", rootNode);

        mockMvc.perform(patch("/api/incidents/{id}", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should not update"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Closed incidents cannot be edited"));
    }

    @Test
    void lifecycleEndpointsShouldUpdateStatusTimestampsAndTimelineMessages() throws Exception {
        NetworkNode rootNode = saveNode("CORE-SBC-WAW-01", NodeType.SBC, "SLASKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-KAT-01", NodeType.G_NODE_B, "SLASKIE");

        Long incidentId = createIncidentThroughApi(rootNode, affectedNode, "INC-103");

        mockMvc.perform(patch("/api/incidents/{id}/acknowledge", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedAt").exists());

        mockMvc.perform(patch("/api/incidents/{id}/resolve", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Traffic rerouted"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        mockMvc.perform(patch("/api/incidents/{id}/close", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").exists());

        Incident incident = incidentRepository.findById(incidentId).orElseThrow();
        List<IncidentTimeline> timeline = incidentTimelineRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getAcknowledgedAt()).isNotNull();
        assertThat(incident.getResolvedAt()).isNotNull();
        assertThat(incident.getClosedAt()).isNotNull();
        assertThat(timeline).hasSize(4);
        assertThat(timeline.stream().map(IncidentTimeline::getEventType))
                .containsExactly(
                        IncidentTimelineEventType.CREATED,
                        IncidentTimelineEventType.ACKNOWLEDGED,
                        IncidentTimelineEventType.RESOLVED,
                        IncidentTimelineEventType.CLOSED
                );
        assertThat(timeline.stream().map(IncidentTimeline::getMessage))
                .containsExactly(
                        "Incident created",
                        "Incident acknowledged",
                        "Incident resolved: Traffic rerouted",
                        "Incident closed"
                );
    }

    @Test
    void closeIncidentShouldRejectInvalidStatusTransition() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-KRK-01", NodeType.ROUTER, "MALOPOLSKIE");
        Incident incident = saveIncident("INC-104", "Still open", IncidentStatus.OPEN, IncidentPriority.LOW, "MALOPOLSKIE", rootNode);

        mockMvc.perform(patch("/api/incidents/{id}/close", incident.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only RESOLVED incidents can be closed"));
    }

    @Test
    void acknowledgeIncidentShouldReturnNotFoundForMissingIncident() throws Exception {
        mockMvc.perform(patch("/api/incidents/{id}/acknowledge", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found: 9999"));
    }

    @Test
    void getIncidentByIdShouldReturnNotFoundForMissingIncident() throws Exception {
        mockMvc.perform(get("/api/incidents/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found: 9999"));
    }

    @Test
    void getIncidentTimelineShouldReturnEventsOrderedByCreatedAtAscending() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-POZ-01", NodeType.ROUTER, "WIELKOPOLSKIE");
        Incident incident = saveIncident("INC-105", "Timeline order", IncidentStatus.RESOLVED, IncidentPriority.HIGH, "WIELKOPOLSKIE", rootNode);

        LocalDateTime now = LocalDateTime.now();
        saveTimelineEvent(incident, "RESOLVED", "Resolved third", now.minusHours(1));
        saveTimelineEvent(incident, "CREATED", "Created first", now.minusHours(3));
        saveTimelineEvent(incident, "ACKNOWLEDGED", "Acknowledged second", now.minusHours(2));

        mockMvc.perform(get("/api/incidents/{id}/timeline", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$[2].eventType").value("RESOLVED"));
    }

    @Test
    void getIncidentTimelineShouldReturnNotFoundForMissingIncident() throws Exception {
        mockMvc.perform(get("/api/incidents/{id}/timeline", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found: 9999"));
    }

    @Test
    void getNetworkNodesShouldFilterAndSortLookupData() throws Exception {
        saveNode("CORE-RTR-WAW-21", NodeType.ROUTER, "MAZOWIECKIE");
        saveNode("RAN-GNB-WAW-22", NodeType.G_NODE_B, "MAZOWIECKIE");
        saveNode("CORE-SBC-KRK-21", NodeType.SBC, "MALOPOLSKIE");

        mockMvc.perform(get("/api/network-nodes")
                        .param("q", "WAW")
                        .param("region", "mazowieckie")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeName").value("CORE-RTR-WAW-21"))
                .andExpect(jsonPath("$[1].nodeName").value("RAN-GNB-WAW-22"));

        mockMvc.perform(get("/api/network-nodes")
                        .param("nodeType", "router"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeType").value("ROUTER"));
    }

    @Test
    void getAllIncidentsShouldFilterCaseInsensitiveRegionAndSortByIncidentNumber() throws Exception {
        NetworkNode warsawRoot = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode warsawGnb = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");
        NetworkNode katowiceRoot = saveNode("CORE-SBC-KAT-01", NodeType.SBC, "SLASKIE");

        saveIncident("INC-201", "Second in sort order", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", warsawRoot);
        saveIncident("INC-200", "First in sort order", IncidentStatus.OPEN, IncidentPriority.CRITICAL, "MAZOWIECKIE", warsawGnb);
        saveIncident("INC-300", "Filtered out by region", IncidentStatus.OPEN, IncidentPriority.HIGH, "SLASKIE", katowiceRoot);
        saveIncident("INC-400", "Filtered out by status", IncidentStatus.ACKNOWLEDGED, IncidentPriority.HIGH, "MAZOWIECKIE", warsawRoot);

        mockMvc.perform(get("/api/incidents")
                        .param("region", "mazowieckie")
                        .param("status", "OPEN")
                        .param("sortBy", "incidentNumber")
                        .param("direction", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].incidentNumber").value("INC-200"))
                .andExpect(jsonPath("$.content[1].incidentNumber").value("INC-201"));
    }

    @Test
    void getAllIncidentsShouldReturnRequestedPageWithPaginationMetadata() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");

        saveIncident("INC-501", "Incident 1", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);
        saveIncident("INC-502", "Incident 2", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);
        saveIncident("INC-503", "Incident 3", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);
        saveIncident("INC-504", "Incident 4", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);
        saveIncident("INC-505", "Incident 5", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE", rootNode);

        mockMvc.perform(get("/api/incidents")
                        .param("sortBy", "incidentNumber")
                        .param("direction", "asc")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].incidentNumber").value("INC-503"))
                .andExpect(jsonPath("$.content[1].incidentNumber").value("INC-504"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getAllIncidentsShouldFilterByOpenedAtRange() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-02", NodeType.ROUTER, "MAZOWIECKIE");

        saveIncident("INC-601", "Before range", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE",
                rootNode, LocalDateTime.of(2026, 3, 29, 7, 0));
        saveIncident("INC-602", "Inside range first", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE",
                rootNode, LocalDateTime.of(2026, 3, 29, 9, 0));
        saveIncident("INC-603", "Inside range second", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE",
                rootNode, LocalDateTime.of(2026, 3, 29, 11, 0));
        saveIncident("INC-604", "After range", IncidentStatus.OPEN, IncidentPriority.HIGH, "MAZOWIECKIE",
                rootNode, LocalDateTime.of(2026, 3, 29, 13, 0));

        mockMvc.perform(get("/api/incidents")
                        .param("openedFrom", "2026-03-29T08:00:00")
                        .param("openedTo", "2026-03-29T12:00:00")
                        .param("sortBy", "openedAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].incidentNumber").value("INC-602"))
                .andExpect(jsonPath("$.content[1].incidentNumber").value("INC-603"));
    }

    @Test
    void getAllIncidentsShouldFilterByIncidentNumberTitleSourceAlarmTypeAndLifecycleRanges() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-LUB-01", NodeType.ROUTER, "LUBELSKIE");

        saveIncident(
                "INC-701",
                "Router failure in Lublin",
                IncidentStatus.CLOSED,
                IncidentPriority.CRITICAL,
                "LUBELSKIE",
                rootNode,
                "HARDWARE",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0)
        );
        saveIncident(
                "INC-702",
                "Router failure in Krakow",
                IncidentStatus.CLOSED,
                IncidentPriority.CRITICAL,
                "MALOPOLSKIE",
                rootNode,
                "POWER",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0)
        );
        saveIncident(
                "NOPE-703",
                "Router failure in Lublin",
                IncidentStatus.CLOSED,
                IncidentPriority.CRITICAL,
                "LUBELSKIE",
                rootNode,
                "HARDWARE",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 11, 0),
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 13, 0)
        );

        mockMvc.perform(get("/api/incidents")
                        .param("incidentNumber", "inc-70")
                        .param("title", "lublin")
                        .param("sourceAlarmType", "hardware")
                        .param("acknowledgedFrom", "2026-03-29T07:30:00")
                        .param("acknowledgedTo", "2026-03-29T08:30:00")
                        .param("resolvedFrom", "2026-03-29T08:30:00")
                        .param("resolvedTo", "2026-03-29T09:30:00")
                        .param("closedFrom", "2026-03-29T09:30:00")
                        .param("closedTo", "2026-03-29T10:30:00")
                        .param("sortBy", "incidentNumber")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].incidentNumber").value("INC-701"));
    }

    @Test
    void getAllIncidentsShouldFilterByMultipleStatusesPrioritiesAndSortByClosedAt() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-BIA-01", NodeType.ROUTER, "PODLASKIE");

        saveIncident(
                "INC-801",
                "First closed",
                IncidentStatus.CLOSED,
                IncidentPriority.CRITICAL,
                "PODLASKIE",
                rootNode,
                "HARDWARE",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0)
        );
        saveIncident(
                "INC-802",
                "Second resolved",
                IncidentStatus.RESOLVED,
                IncidentPriority.HIGH,
                "PODLASKIE",
                rootNode,
                "POWER",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 9, 30),
                null
        );
        saveIncident(
                "INC-803",
                "Third closed later",
                IncidentStatus.CLOSED,
                IncidentPriority.CRITICAL,
                "PODLASKIE",
                rootNode,
                "POWER",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 11, 0)
        );
        saveIncident(
                "INC-804",
                "Filtered by priority",
                IncidentStatus.CLOSED,
                IncidentPriority.LOW,
                "PODLASKIE",
                rootNode,
                "POWER",
                false,
                LocalDateTime.of(2026, 3, 29, 7, 0),
                LocalDateTime.of(2026, 3, 29, 8, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 12, 0)
        );

        mockMvc.perform(get("/api/incidents")
                        .param("statuses", "CLOSED,RESOLVED")
                        .param("priorities", "CRITICAL,HIGH")
                        .param("sortBy", "closedAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].incidentNumber").value("INC-803"))
                .andExpect(jsonPath("$.content[1].incidentNumber").value("INC-801"))
                .andExpect(jsonPath("$.content[2].incidentNumber").value("INC-802"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForInvalidMultiValueStatusFilter() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("statuses", "OPEN,INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'INVALID' for parameter 'statuses'"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForUnsupportedSortBy() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sortBy value: createdAt"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForUnsupportedDirection() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported direction value: sideways"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForNegativePage() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors['getAllIncidents.page']").value("must be greater than or equal to 0"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForSizeGreaterThanMaximum() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors['getAllIncidents.size']").value("must be less than or equal to 100"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForInvalidPriorityEnum() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'URGENT' for parameter 'priority'"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestWhenOpenedFromIsAfterOpenedTo() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("openedFrom", "2026-03-29T12:00:00")
                        .param("openedTo", "2026-03-29T08:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("openedFrom must be earlier than or equal to openedTo"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestWhenClosedFromIsAfterClosedTo() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("closedFrom", "2026-03-29T12:00:00")
                        .param("closedTo", "2026-03-29T08:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("closedFrom must be earlier than or equal to closedTo"));
    }

    private Long createIncidentThroughApi(NetworkNode rootNode, NetworkNode affectedNode, String incidentNumber) throws Exception {
        String requestBody = """
                {
                  "incidentNumber": "%s",
                  "title": "Lifecycle test incident",
                  "priority": "CRITICAL",
                  "region": "SLASKIE",
                  "rootNodeId": %d,
                  "nodes": [
                    {
                      "networkNodeId": %d,
                      "role": "ROOT"
                    },
                    {
                      "networkNodeId": %d,
                      "role": "AFFECTED"
                    }
                  ]
                }
                """.formatted(incidentNumber, rootNode.getId(), rootNode.getId(), affectedNode.getId());

        String responseBody = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asLong();
    }

    private NetworkNode saveNode(String nodeName, NodeType nodeType, String region) {
        NetworkNode node = NetworkNode.builder()
                .nodeName(nodeName)
                .nodeType(nodeType)
                .region(Region.valueOf(region))
                .vendor("TestVendor")
                .active(true)
                .build();

        return networkNodeRepository.saveAndFlush(node);
    }

    private Incident saveIncident(
            String incidentNumber,
            String title,
            IncidentStatus status,
            IncidentPriority priority,
            String region,
            NetworkNode rootNode
    ) {
        return saveIncident(
                incidentNumber,
                title,
                status,
                priority,
                region,
                rootNode,
                SourceAlarmType.HARDWARE.name(),
                false,
                LocalDateTime.now().minusHours(2),
                status == IncidentStatus.ACKNOWLEDGED || status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? LocalDateTime.now().minusHours(1)
                        : null,
                status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? LocalDateTime.now()
                        : null,
                status == IncidentStatus.CLOSED
                        ? LocalDateTime.now().plusHours(1)
                        : null
        );
    }

    private Incident saveIncident(
            String incidentNumber,
            String title,
            IncidentStatus status,
            IncidentPriority priority,
            String region,
            NetworkNode rootNode,
            LocalDateTime openedAt
    ) {
        return saveIncident(
                incidentNumber,
                title,
                status,
                priority,
                region,
                rootNode,
                SourceAlarmType.HARDWARE.name(),
                false,
                openedAt,
                status == IncidentStatus.ACKNOWLEDGED || status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? openedAt.plusHours(1)
                        : null,
                status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? openedAt.plusHours(2)
                        : null,
                status == IncidentStatus.CLOSED
                        ? openedAt.plusHours(3)
                        : null
        );
    }

    private Incident saveIncident(
            String incidentNumber,
            String title,
            IncidentStatus status,
            IncidentPriority priority,
            String region,
            NetworkNode rootNode,
            String sourceAlarmType,
            Boolean possiblyPlanned,
            LocalDateTime openedAt,
            LocalDateTime acknowledgedAt,
            LocalDateTime resolvedAt,
            LocalDateTime closedAt
    ) {
        Incident incident = Incident.builder()
                .incidentNumber(incidentNumber)
                .title(title)
                .status(status)
                .priority(priority)
                .region(Region.valueOf(region))
                .sourceAlarmType(SourceAlarmType.valueOf(sourceAlarmType))
                .possiblyPlanned(possiblyPlanned)
                .rootNode(rootNode)
                .openedAt(openedAt)
                .acknowledgedAt(acknowledgedAt)
                .resolvedAt(resolvedAt)
                .closedAt(closedAt)
                .build();

        IncidentNode rootIncidentNode = new IncidentNode();
        rootIncidentNode.setNetworkNode(rootNode);
        rootIncidentNode.setRole(IncidentNodeRole.ROOT);
        incident.addIncidentNode(rootIncidentNode);

        return incidentRepository.saveAndFlush(incident);
    }

    private void saveTimelineEvent(Incident incident, String eventType, String message, LocalDateTime createdAt) {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(IncidentTimelineEventType.valueOf(eventType));
        timeline.setMessage(message);
        timeline.setCreatedAt(createdAt);
        incidentTimelineRepository.saveAndFlush(timeline);
    }
}
