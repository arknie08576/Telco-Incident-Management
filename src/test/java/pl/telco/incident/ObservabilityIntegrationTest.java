package pl.telco.incident;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.repository.NetworkNodeRepository;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NetworkNodeRepository networkNodeRepository;

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
