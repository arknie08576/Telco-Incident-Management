package pl.telco.incident;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsShouldExposeIncidentEndpointsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Telco Incident Management API"))
                .andExpect(jsonPath("$.paths['/api/incidents'].post.summary").value("Create incident"))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.summary").value("List incidents"))
                .andExpect(jsonPath("$.paths['/api/incidents/{id}'].patch.summary").value("Update incident"))
                .andExpect(jsonPath("$.paths['/api/network-nodes'].post.summary").value("Create network node"))
                .andExpect(jsonPath("$.paths['/api/network-nodes'].get.summary").value("List network nodes"))
                .andExpect(jsonPath("$.paths['/api/network-nodes/{id}'].patch.summary").value("Update network node"))
                .andExpect(jsonPath("$.paths['/api/maintenance-windows'].post.summary").value("Create maintenance window"))
                .andExpect(jsonPath("$.paths['/api/maintenance-windows'].get.summary").value("List maintenance windows"))
                .andExpect(jsonPath("$.paths['/api/maintenance-windows/{id}'].patch.summary").value("Update maintenance window"))
                .andExpect(jsonPath("$.paths['/api/alarm-events'].post.summary").value("Create alarm event"))
                .andExpect(jsonPath("$.paths['/api/alarm-events'].get.summary").value("List alarm events"))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("openedFrom")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("openedTo")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("priorities")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("statuses")))
                .andExpect(jsonPath("$.paths['/api/incidents/{id}/timeline'].get.summary").value("Get incident timeline"))
                .andExpect(jsonPath("$.components.schemas.IncidentCreateRequest.required", hasItem("incidentNumber")))
                .andExpect(jsonPath("$.components.schemas.IncidentUpdateRequest.properties.rootNodeId.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.IncidentUpdateRequest.properties.nodes.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.IncidentResponse.properties.nodes.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.MaintenanceWindowUpdateRequest.properties.nodeIds.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.IncidentSummaryResponse.properties.region.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.IncidentPageResponse.properties.totalElements.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.message.type").value("string"));
    }

    @Test
    void swaggerUiShortcutShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
