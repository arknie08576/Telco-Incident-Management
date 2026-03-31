package pl.telco.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlarmEventApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NetworkNodeRepository networkNodeRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM alarm_event");
        jdbcTemplate.update("DELETE FROM maintenance_node");
        jdbcTemplate.update("DELETE FROM maintenance_window");
        jdbcTemplate.update("DELETE FROM incident_timeline");
        jdbcTemplate.update("DELETE FROM incident_node");
        jdbcTemplate.update("DELETE FROM incident");
        networkNodeRepository.deleteAllInBatch();
    }

    @Test
    void updateAlarmEventShouldPatchFieldsAndSupportGetByIdAndListing() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-GDA-61", NodeType.ROUTER, Region.MAZOWIECKIE);
        NetworkNode alarmNode = saveNode("RAN-GNB-GDA-62", NodeType.G_NODE_B, Region.MAZOWIECKIE);
        long incidentId = createIncident(rootNode.getId());

        String alarmResponse = mockMvc.perform(post("/api/alarm-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "DEMO",
                                  "externalId": "ALARM-LIST-01",
                                  "networkNodeId": %d,
                                  "alarmType": "BGP_FLAP",
                                  "severity": "MAJOR",
                                  "status": "OPEN",
                                  "description": "Original alarm",
                                  "suppressedByMaintenance": false,
                                  "occurredAt": "2099-01-01T09:55:00"
                                }
                                """.formatted(alarmNode.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alarmEventId = objectMapper.readTree(alarmResponse).get("id").asLong();

        mockMvc.perform(patch("/api/alarm-events/{id}", alarmEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentId": %d,
                                  "severity": "CRITICAL",
                                  "status": "ACKNOWLEDGED",
                                  "description": "Updated alarm",
                                  "suppressedByMaintenance": true,
                                  "occurredAt": "2099-01-01T10:00:00"
                                }
                                """.formatted(incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alarmEventId))
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.suppressedByMaintenance").value(true))
                .andExpect(jsonPath("$.description").value("Updated alarm"));

        mockMvc.perform(get("/api/alarm-events/{id}", alarmEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alarmEventId))
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        mockMvc.perform(get("/api/alarm-events")
                        .param("page", "0")
                        .param("size", "10")
                        .param("severity", "CRITICAL")
                        .param("statuses", "ACKNOWLEDGED")
                        .param("sourceSystem", "DEMO")
                        .param("externalId", "ALARM-LIST")
                        .param("alarmType", "BGP")
                        .param("networkNodeId", String.valueOf(alarmNode.getId()))
                        .param("incidentId", String.valueOf(incidentId))
                        .param("suppressedByMaintenance", "true")
                        .param("sortBy", "occurredAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alarmEventId))
                .andExpect(jsonPath("$.content[0].status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"));
    }

    @Test
    void updateAlarmEventShouldRejectNoOpPatch() throws Exception {
        NetworkNode alarmNode = saveNode("RAN-GNB-LDZ-71", NodeType.G_NODE_B, Region.MALOPOLSKIE);

        String alarmResponse = mockMvc.perform(post("/api/alarm-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "DEMO",
                                  "externalId": "ALARM-NOOP-01",
                                  "networkNodeId": %d,
                                  "alarmType": "CARD_FAILURE",
                                  "severity": "MAJOR",
                                  "status": "OPEN",
                                  "description": "Alarm without change",
                                  "suppressedByMaintenance": false,
                                  "occurredAt": "2099-01-02T09:55:00"
                                }
                                """.formatted(alarmNode.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alarmEventId = objectMapper.readTree(alarmResponse).get("id").asLong();

        mockMvc.perform(patch("/api/alarm-events/{id}", alarmEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patch request does not change alarm event"));
    }

    private long createIncident(Long rootNodeId) throws Exception {
        String responseBody = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-ALARM-01",
                                  "title": "Incident used for alarm correlation",
                                  "priority": "HIGH",
                                  "region": "MAZOWIECKIE",
                                  "sourceAlarmType": "NETWORK",
                                  "possiblyPlanned": false,
                                  "rootNodeId": %d,
                                  "nodes": [
                                    {
                                      "networkNodeId": %d,
                                      "role": "ROOT"
                                    }
                                  ]
                                }
                                """.formatted(rootNodeId, rootNodeId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asLong();
    }

    private NetworkNode saveNode(String nodeName, NodeType nodeType, Region region) {
        return networkNodeRepository.saveAndFlush(NetworkNode.builder()
                .nodeName(nodeName)
                .nodeType(nodeType)
                .region(region)
                .vendor("TestVendor")
                .active(true)
                .build());
    }
}
