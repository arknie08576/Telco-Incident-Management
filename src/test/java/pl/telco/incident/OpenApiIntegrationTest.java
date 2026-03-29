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
                .andExpect(jsonPath("$.paths['/api/incidents/{id}/timeline'].get.summary").value("Get incident timeline"))
                .andExpect(jsonPath("$.components.schemas.IncidentCreateRequest.required", hasItem("incidentNumber")))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.message.type").value("string"));
    }

    @Test
    void swaggerUiShortcutShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
