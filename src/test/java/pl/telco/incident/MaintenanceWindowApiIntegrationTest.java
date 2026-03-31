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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceWindowApiIntegrationTest extends AbstractPostgresIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM maintenance_node");
        jdbcTemplate.update("DELETE FROM maintenance_window");
        networkNodeRepository.deleteAllInBatch();
    }

    @Test
    void updateMaintenanceWindowShouldPatchFieldsAndReplaceNodeLinks() throws Exception {
        NetworkNode firstNode = saveNode("CORE-RTR-WAW-41", NodeType.ROUTER, Region.MAZOWIECKIE);
        NetworkNode secondNode = saveNode("CORE-SBC-WAW-42", NodeType.SBC, Region.MAZOWIECKIE);
        NetworkNode thirdNode = saveNode("RAN-GNB-WAW-43", NodeType.G_NODE_B, Region.MAZOWIECKIE);

        LocalDateTime initialStart = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime initialEnd = initialStart.plusHours(2);

        Long maintenanceWindowId = createMaintenanceWindow(firstNode.getId(), secondNode.getId(), initialStart, initialEnd);

        LocalDateTime updatedStart = initialStart.plusDays(1);
        LocalDateTime updatedEnd = updatedStart.plusHours(4);

        mockMvc.perform(patch("/api/maintenance-windows/{id}", maintenanceWindowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated maintenance",
                                  "status": "IN_PROGRESS",
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "nodeIds": [%d, %d]
                                }
                                """.formatted(updatedStart, updatedEnd, secondNode.getId(), thirdNode.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maintenanceWindowId))
                .andExpect(jsonPath("$.title").value("Updated maintenance"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.nodeIds.length()").value(2))
                .andExpect(jsonPath("$.nodeIds[0]").value(secondNode.getId()))
                .andExpect(jsonPath("$.nodeIds[1]").value(thirdNode.getId()));

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM maintenance_window WHERE id = ?",
                String.class,
                maintenanceWindowId
        );
        List<Long> linkedNodeIds = jdbcTemplate.query(
                "SELECT network_node_id FROM maintenance_node WHERE maintenance_window_id = ? ORDER BY id",
                (rs, rowNum) -> rs.getLong("network_node_id"),
                maintenanceWindowId
        );

        assertThat(title).isEqualTo("Updated maintenance");
        assertThat(linkedNodeIds).containsExactly(secondNode.getId(), thirdNode.getId());
    }

    @Test
    void updateMaintenanceWindowShouldRejectNoOpPatch() throws Exception {
        NetworkNode firstNode = saveNode("CORE-RTR-KRK-41", NodeType.ROUTER, Region.MALOPOLSKIE);
        NetworkNode secondNode = saveNode("RAN-GNB-KRK-42", NodeType.G_NODE_B, Region.MALOPOLSKIE);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.plusHours(2);
        Long maintenanceWindowId = createMaintenanceWindow(firstNode.getId(), secondNode.getId(), start, end);

        mockMvc.perform(patch("/api/maintenance-windows/{id}", maintenanceWindowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patch request does not change maintenance window"));
    }

    private Long createMaintenanceWindow(Long firstNodeId, Long secondNodeId, LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        String responseBody = mockMvc.perform(post("/api/maintenance-windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Original maintenance",
                                  "description": "Window before update",
                                  "status": "PLANNED",
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "nodeIds": [%d, %d]
                                }
                                """.formatted(startTime, endTime, firstNodeId, secondNodeId)))
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
