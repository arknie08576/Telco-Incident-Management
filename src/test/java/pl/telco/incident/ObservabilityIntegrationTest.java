package pl.telco.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.repository.NetworkNodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkNodeRepository networkNodeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void actuatorLivenessShouldExposeUpStatus() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorInfoShouldExposeApplicationMetadata() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Telco Incident Management"))
                .andExpect(jsonPath("$.app.description").value("Student telco incident management backend"));
    }

    @Test
    void actuatorMetricsShouldExposeAvailableMeters() throws Exception {
        NetworkNode rootNode = saveNode("CORE-RTR-OBS-01", NodeType.ROUTER, Region.MAZOWIECKIE);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-METRIC-1",
                                  "title": "Metrics incident",
                                  "priority": "HIGH",
                                  "region": "MAZOWIECKIE",
                                  "sourceAlarmType": "HARDWARE",
                                  "possiblyPlanned": false,
                                  "rootNodeId": %d,
                                  "nodes": [
                                    {
                                      "networkNodeId": %d,
                                      "role": "ROOT"
                                    }
                                  ]
                                }
                                """.formatted(rootNode.getId(), rootNode.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("http.server.requests")))
                .andExpect(jsonPath("$.names", hasItem("incident.created")));
    }

    @Test
    void publicApiShouldWriteRowsForRemainingTables() throws Exception {
        String networkNodeResponse = mockMvc.perform(post("/api/network-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeName": "ELK-DEMO-OBS-01",
                                  "nodeType": "G_NODE_B",
                                  "region": "SLASKIE",
                                  "vendor": "DemoVendor",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nodeName").value("ELK-DEMO-OBS-01"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long networkNodeId = objectMapper.readTree(networkNodeResponse).get("id").asLong();

        mockMvc.perform(patch("/api/network-nodes/{id}", networkNodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vendor": "DemoVendorUpdated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendor").value("DemoVendorUpdated"));

        String maintenanceWindowResponse = mockMvc.perform(post("/api/maintenance-windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "ELK demo maintenance public API",
                                  "description": "Created through public API",
                                  "status": "PLANNED",
                                  "startTime": "2099-01-01T10:00:00",
                                  "endTime": "2099-01-01T12:00:00",
                                  "nodeIds": [%d]
                                }
                                """.formatted(networkNodeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nodeIds[0]").value(networkNodeId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode maintenanceWindowJson = objectMapper.readTree(maintenanceWindowResponse);
        long maintenanceWindowId = maintenanceWindowJson.get("id").asLong();
        assertThat(maintenanceWindowId).isPositive();

        String alarmEventResponse = mockMvc.perform(post("/api/alarm-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystem": "DEMO",
                                  "externalId": "ALARM-DEMO-OBS-01",
                                  "networkNodeId": %d,
                                  "alarmType": "BGP_FLAP",
                                  "severity": "MAJOR",
                                  "status": "OPEN",
                                  "description": "Created through public API",
                                  "suppressedByMaintenance": false,
                                  "occurredAt": "2099-01-01T09:55:00"
                                }
                                """.formatted(networkNodeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.externalId").value("ALARM-DEMO-OBS-01"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alarmEventId = objectMapper.readTree(alarmEventResponse).get("id").asLong();

        Integer demoNodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM network_node WHERE node_name LIKE 'ELK-DEMO-%'",
                Integer.class
        );
        Integer maintenanceWindowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM maintenance_window WHERE title LIKE 'ELK demo maintenance %'",
                Integer.class
        );
        Integer maintenanceNodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM maintenance_node",
                Integer.class
        );
        Integer alarmEventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alarm_event WHERE external_id LIKE 'ALARM-DEMO-%'",
                Integer.class
        );

        assertThat(demoNodeCount).isNotNull().isGreaterThan(0);
        assertThat(maintenanceWindowCount).isNotNull().isGreaterThan(0);
        assertThat(maintenanceNodeCount).isNotNull().isGreaterThan(0);
        assertThat(alarmEventCount).isNotNull().isGreaterThan(0);

        mockMvc.perform(get("/api/network-nodes/{id}", networkNodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(networkNodeId));

        mockMvc.perform(get("/api/maintenance-windows/{id}", maintenanceWindowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maintenanceWindowId));

        mockMvc.perform(get("/api/alarm-events/{id}", alarmEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alarmEventId));

        mockMvc.perform(get("/api/maintenance-windows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("ELK demo maintenance public API"));

        mockMvc.perform(get("/api/alarm-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("ALARM-DEMO-OBS-01"));
    }

    private NetworkNode saveNode(String nodeName, NodeType nodeType, Region region) {
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
