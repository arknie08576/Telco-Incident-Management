package pl.telco.incident;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.repository.NetworkNodeRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NetworkNodeApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void getNetworkNodeByIdShouldReturnStoredNode() throws Exception {
        NetworkNode node = networkNodeRepository.saveAndFlush(NetworkNode.builder()
                .nodeName("CORE-RTR-WAW-81")
                .nodeType(NodeType.ROUTER)
                .region(Region.MAZOWIECKIE)
                .vendor("Cisco")
                .active(true)
                .build());

        mockMvc.perform(get("/api/network-nodes/{id}", node.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(node.getId()))
                .andExpect(jsonPath("$.nodeName").value("CORE-RTR-WAW-81"))
                .andExpect(jsonPath("$.nodeType").value("ROUTER"))
                .andExpect(jsonPath("$.region").value("MAZOWIECKIE"));
    }
}
