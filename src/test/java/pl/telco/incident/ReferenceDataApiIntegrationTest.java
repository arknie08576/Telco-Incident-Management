package pl.telco.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDataApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlarmEventRepository alarmEventRepository;

    @Autowired
    private IncidentTimelineRepository incidentTimelineRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private MaintenanceWindowRepository maintenanceWindowRepository;

    @Autowired
    private NetworkNodeRepository networkNodeRepository;

    @BeforeEach
    void cleanDatabase() {
        alarmEventRepository.deleteAllInBatch();
        incidentTimelineRepository.deleteAllInBatch();
        incidentRepository.deleteAllInBatch();
        maintenanceWindowRepository.deleteAllInBatch();
        networkNodeRepository.deleteAllInBatch();
    }

    @Test
    void networkNodeCrudShouldWork() throws Exception {
        String createBody = """
                {
                  "nodeName": "CORE-RTR-WAW-02",
                  "nodeType": "ROUTER",
                  "region": "MAZOWIECKIE",
                  "vendor": "Cisco",
                  "active": true
                }
                """;

        String createResponse = mockMvc.perform(post("/api/network-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nodeName").value("CORE-RTR-WAW-02"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long nodeId = readId(createResponse);

        mockMvc.perform(get("/api/network-nodes/{id}", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeType").value("ROUTER"));

        mockMvc.perform(put("/api/network-nodes/{id}", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeName": "CORE-RTR-WAW-02A",
                                  "nodeType": "SBC",
                                  "region": "SLASKIE",
                                  "vendor": "Oracle",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeName").value("CORE-RTR-WAW-02A"))
                .andExpect(jsonPath("$.nodeType").value("SBC"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/network-nodes/{id}", nodeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/network-nodes/{id}", nodeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void maintenanceWindowCrudShouldWork() throws Exception {
        NetworkNode rootNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-02", NodeType.G_NODE_B, "MAZOWIECKIE");

        String createResponse = mockMvc.perform(post("/api/maintenance-windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Planned RAN upgrade",
                                  "description": "Upgrade window",
                                  "status": "PLANNED",
                                  "startTime": "2026-03-31T08:00:00",
                                  "endTime": "2026-03-31T12:00:00",
                                  "networkNodeIds": [%d, %d]
                                }
                                """.formatted(rootNode.getId(), affectedNode.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.networkNodeIds.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long windowId = readId(createResponse);

        mockMvc.perform(put("/api/maintenance-windows/{id}", windowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Planned RAN upgrade updated",
                                  "description": "Updated window",
                                  "status": "IN_PROGRESS",
                                  "startTime": "2026-03-31T09:00:00",
                                  "endTime": "2026-03-31T13:00:00",
                                  "networkNodeIds": [%d]
                                }
                                """.formatted(rootNode.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.networkNodeIds.length()").value(1));

        mockMvc.perform(delete("/api/maintenance-windows/{id}", windowId))
                .andExpect(status().isNoContent());
    }

    @Test
    void alarmEventCrudShouldWork() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");
        long incidentId = createIncident(rootNode.getId(), rootNode.getId(), affectedNode.getId(), "INC-ALARM-1");

        String createResponse = mockMvc.perform(post("/api/alarm-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "OSS",
                                  "externalId": "ALARM-100",
                                  "networkNodeId": %d,
                                  "incidentId": %d,
                                  "alarmType": "LINK_DOWN",
                                  "severity": "MAJOR",
                                  "status": "OPEN",
                                  "description": "Core uplink down",
                                  "suppressedByMaintenance": false,
                                  "occurredAt": "2026-03-30T10:15:00",
                                  "receivedAt": "2026-03-30T10:16:00"
                                }
                                """.formatted(rootNode.getId(), incidentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").value("ALARM-100"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alarmId = readId(createResponse);

        mockMvc.perform(put("/api/alarm-events/{id}", alarmId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "OSS",
                                  "externalId": "ALARM-100",
                                  "networkNodeId": %d,
                                  "incidentId": %d,
                                  "alarmType": "LINK_DOWN",
                                  "severity": "CRITICAL",
                                  "status": "ACKNOWLEDGED",
                                  "description": "Acknowledged by NOC",
                                  "suppressedByMaintenance": false,
                                  "occurredAt": "2026-03-30T10:15:00",
                                  "receivedAt": "2026-03-30T10:16:00"
                                }
                                """.formatted(rootNode.getId(), incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        mockMvc.perform(delete("/api/alarm-events/{id}", alarmId))
                .andExpect(status().isNoContent());
    }

    @Test
    void incidentPatchShouldUpdateRootNodeAndNodes() throws Exception {
        NetworkNode firstNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode secondNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");

        long incidentId = createIncident(firstNode.getId(), firstNode.getId(), secondNode.getId(), "INC-UPD-1");

        mockMvc.perform(patch("/api/incidents/{id}", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rootNodeId": %d,
                                  "nodes": [
                                    { "networkNodeId": %d, "role": "ROOT" },
                                    { "networkNodeId": %d, "role": "AFFECTED" }
                                  ]
                                }
                                """.formatted(secondNode.getId(), secondNode.getId(), firstNode.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootNodeId").value(secondNode.getId()))
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].role").value("ROOT"));
    }

    @Test
    void incidentTimelineCrudAndDeleteShouldWork() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-WAW-01", NodeType.ROUTER, "MAZOWIECKIE");
        NetworkNode affectedNode = saveNode("RAN-GNB-WAW-01", NodeType.G_NODE_B, "MAZOWIECKIE");
        long incidentId = createIncident(rootNode.getId(), rootNode.getId(), affectedNode.getId(), "INC-TL-1");

        String timelineResponse = mockMvc.perform(post("/api/incidents/{id}/timeline", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "MANUAL_NOTE",
                                  "message": "Operator note"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("MANUAL_NOTE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long timelineId = readId(timelineResponse);

        mockMvc.perform(put("/api/incidents/{id}/timeline/{timelineId}", incidentId, timelineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "MANUAL_NOTE",
                                  "message": "Operator note updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Operator note updated"));

        mockMvc.perform(delete("/api/incidents/{id}/timeline/{timelineId}", incidentId, timelineId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/incidents/{id}", incidentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/incidents/{id}", incidentId))
                .andExpect(status().isNotFound());
    }

    private long createIncident(long rootNodeId, long rootIncidentNodeId, long affectedNodeId, String incidentNumber) throws Exception {
        String responseBody = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "%s",
                                  "title": "Integration incident",
                                  "priority": "HIGH",
                                  "region": "MAZOWIECKIE",
                                  "sourceAlarmType": "POWER",
                                  "possiblyPlanned": false,
                                  "rootNodeId": %d,
                                  "nodes": [
                                    { "networkNodeId": %d, "role": "ROOT" },
                                    { "networkNodeId": %d, "role": "AFFECTED" }
                                  ]
                                }
                                """.formatted(incidentNumber, rootNodeId, rootIncidentNodeId, affectedNodeId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(responseBody);
    }

    private long readId(String responseBody) throws Exception {
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
}
