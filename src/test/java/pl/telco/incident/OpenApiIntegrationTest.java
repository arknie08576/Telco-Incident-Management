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
    void apiDocsShouldExposeIncidentAndReferenceDataEndpointsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Telco Incident Management API"))
                .andExpect(jsonPath("$.paths['/api/incidents'].post.summary").value("Create incident"))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.summary").value("List incidents"))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("openedFrom")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("openedTo")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("priorities")))
                .andExpect(jsonPath("$.paths['/api/incidents'].get.parameters[*].name", hasItem("statuses")))
                .andExpect(jsonPath("$.paths['/api/incidents/{id}/timeline'].get.summary").value("Get incident timeline"))
                .andExpect(jsonPath("$.paths['/api/network-nodes'].post.summary").value("Create network node"))
                .andExpect(jsonPath("$.paths['/api/maintenance-windows/{id}'].put.summary").value("Update maintenance window"))
                .andExpect(jsonPath("$.paths['/api/alarm-events'].get.summary").value("List alarm events"))
                .andExpect(jsonPath("$.paths['/api/incident-nodes'].post.summary").value("Create incident_node row"))
                .andExpect(jsonPath("$.paths['/api/maintenance-nodes'].post.summary").value("Create maintenance_node row"))
                .andExpect(jsonPath("$.paths['/api/incident-timeline'].post.summary").value("Create incident_timeline row"))
                .andExpect(jsonPath("$.components.schemas.IncidentCreateRequest.required", hasItem("incidentNumber")))
                .andExpect(jsonPath("$.components.schemas.NetworkNodeRequest.required", hasItem("nodeName")))
                .andExpect(jsonPath("$.components.schemas.MaintenanceWindowRequest.required", hasItem("title")))
                .andExpect(jsonPath("$.components.schemas.AlarmEventRequest.required", hasItem("sourceSystem")))
                .andExpect(jsonPath("$.components.schemas.IncidentNodeCrudRequest.required", hasItem("incidentId")))
                .andExpect(jsonPath("$.components.schemas.MaintenanceNodeRequest.required", hasItem("maintenanceWindowId")))
                .andExpect(jsonPath("$.components.schemas.IncidentTimelineEntryRequest.required", hasItem("eventType")))
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
