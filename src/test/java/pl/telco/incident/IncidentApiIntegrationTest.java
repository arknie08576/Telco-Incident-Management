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
import pl.telco.incident.entity.enums.NodeType;
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
        assertThat(timeline.getFirst().getEventType()).isEqualTo("CREATED");
        assertThat(timeline.getFirst().getMessage()).isEqualTo("Incident created");
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
                .containsExactly("CREATED", "ACKNOWLEDGED", "RESOLVED", "CLOSED");
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
                .region(region)
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
        Incident incident = Incident.builder()
                .incidentNumber(incidentNumber)
                .title(title)
                .status(status)
                .priority(priority)
                .region(region)
                .sourceAlarmType("TEST")
                .possiblyPlanned(false)
                .rootNode(rootNode)
                .openedAt(LocalDateTime.now().minusHours(2))
                .acknowledgedAt(status == IncidentStatus.ACKNOWLEDGED || status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? LocalDateTime.now().minusHours(1)
                        : null)
                .resolvedAt(status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED
                        ? LocalDateTime.now().minusMinutes(30)
                        : null)
                .closedAt(status == IncidentStatus.CLOSED
                        ? LocalDateTime.now().minusMinutes(10)
                        : null)
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
        timeline.setEventType(eventType);
        timeline.setMessage(message);
        timeline.setCreatedAt(createdAt);
        incidentTimelineRepository.saveAndFlush(timeline);
    }
}
