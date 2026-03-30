package pl.telco.incident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.dto.IncidentCreateRequest;
import pl.telco.incident.dto.IncidentActionRequest;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.dto.IncidentUpdateRequest;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.GlobalExceptionHandler;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.service.IncidentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IncidentController.class)
@Import({GlobalExceptionHandler.class, IncidentControllerWebMvcTest.TestConfig.class})
class IncidentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeIncidentService incidentService;

    @Test
    void createIncidentShouldReturnValidationErrorsForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "incidentNumber": "",
                  "title": "",
                  "region": "",
                  "rootNodeId": 0,
                  "nodes": []
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.incidentNumber").value("incidentNumber is required"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title is required"))
                .andExpect(jsonPath("$.fieldErrors.priority").value("priority is required"))
                .andExpect(jsonPath("$.fieldErrors.region").value("region is required"))
                .andExpect(jsonPath("$.fieldErrors.rootNodeId").value("rootNodeId must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.nodes").value("nodes must not be empty"));
    }

    @Test
    void createIncidentShouldReturnBadRequestForInvalidEnumInJsonBody() throws Exception {
        String requestBody = """
                {
                  "incidentNumber": "INC-500",
                  "title": "Invalid enum",
                  "priority": "URGENT",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": 1,
                  "nodes": [
                    {
                      "networkNodeId": 1,
                      "role": "ROOT"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid enum value"));
    }

    @Test
    void getAllIncidentsShouldReturnValidationErrorForInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors['getAllIncidents.page']").value("must be greater than or equal to 0"))
                .andExpect(jsonPath("$.fieldErrors['getAllIncidents.size']").value("must be greater than or equal to 1"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForInvalidEnumInQueryParam() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'URGENT' for parameter 'priority'"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForInvalidOpenedFromFormat() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("openedFrom", "2026-03-29"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value '2026-03-29' for parameter 'openedFrom'"));
    }

    @Test
    void getAllIncidentsShouldReturnBadRequestForInvalidResolvedFromFormat() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .param("resolvedFrom", "2026-03-29"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value '2026-03-29' for parameter 'resolvedFrom'"));
    }

    @Test
    void getIncidentByIdShouldMapResourceNotFoundException() throws Exception {
        incidentService.setGetIncidentByIdHandler(id -> {
            throw new ResourceNotFoundException("Incident not found: " + id);
        });

        mockMvc.perform(get("/api/incidents/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found: 999"));
    }

    @Test
    void createIncidentShouldMapConflictException() throws Exception {
        String requestBody = """
                {
                  "incidentNumber": "INC-501",
                  "title": "Duplicate number",
                  "priority": "HIGH",
                  "region": "MAZOWIECKIE",
                  "rootNodeId": 1,
                  "nodes": [
                    {
                      "networkNodeId": 1,
                      "role": "ROOT"
                    }
                  ]
                }
                """;

        incidentService.setCreateIncidentHandler(request -> {
            throw new ConflictException("Incident with number already exists: INC-501");
        });

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incident with number already exists: INC-501"));
    }

    @Test
    void acknowledgeIncidentShouldMapBadRequestException() throws Exception {
        incidentService.setAcknowledgeIncidentHandler((id, request) -> {
            throw new BadRequestException("Only OPEN incidents can be acknowledged");
        });

        mockMvc.perform(patch("/api/incidents/{id}/acknowledge", 10L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only OPEN incidents can be acknowledged"));
    }

    @Test
    void updateIncidentShouldReturnValidationErrorsForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "title": "   ",
                  "region": "",
                  "sourceAlarmType": "   "
                }
                """;

        mockMvc.perform(patch("/api/incidents/{id}", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.region").value("region must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.sourceAlarmType").value("sourceAlarmType must not be blank"));
    }

    @Test
    void updateIncidentShouldMapConflictException() throws Exception {
        incidentService.setUpdateIncidentHandler((id, request) -> {
            throw new ConflictException("Incident with number already exists: INC-700");
        });

        mockMvc.perform(patch("/api/incidents/{id}", 31L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentNumber": "INC-700"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incident with number already exists: INC-700"));
    }

    @Test
    void closeIncidentShouldPassOptionalRequestBodyToService() throws Exception {
        IncidentResponse response = new IncidentResponse();
        response.setId(20L);
        response.setIncidentNumber("INC-600");
        response.setTitle("Closed incident");
        response.setStatus(IncidentStatus.CLOSED);
        response.setPriority(IncidentPriority.HIGH);
        response.setRegion("SLASKIE");
        response.setOpenedAt(LocalDateTime.now().minusHours(3));
        response.setClosedAt(LocalDateTime.now());

        incidentService.setCloseIncidentHandler((id, request) -> response);

        mockMvc.perform(patch("/api/incidents/{id}/close", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncidentActionRequest("Verified by NOC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.incidentNumber").value("INC-600"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        FakeIncidentService incidentService() {
            return new FakeIncidentService();
        }
    }

    static class FakeIncidentService extends IncidentService {

        private Function<IncidentCreateRequest, IncidentResponse> createIncidentHandler =
                request -> new IncidentResponse();
        private LongFunction<IncidentResponse> getIncidentByIdHandler =
                id -> new IncidentResponse();
        private BiFunction<Long, IncidentUpdateRequest, IncidentResponse> updateIncidentHandler =
                (id, request) -> new IncidentResponse();
        private BiFunction<Long, IncidentActionRequest, IncidentResponse> acknowledgeIncidentHandler =
                (id, request) -> new IncidentResponse();
        private BiFunction<Long, IncidentActionRequest, IncidentResponse> closeIncidentHandler =
                (id, request) -> new IncidentResponse();

        FakeIncidentService() {
            super(null, null, null);
        }

        void setCreateIncidentHandler(Function<IncidentCreateRequest, IncidentResponse> createIncidentHandler) {
            this.createIncidentHandler = createIncidentHandler;
        }

        void setGetIncidentByIdHandler(LongFunction<IncidentResponse> getIncidentByIdHandler) {
            this.getIncidentByIdHandler = getIncidentByIdHandler;
        }

        void setUpdateIncidentHandler(BiFunction<Long, IncidentUpdateRequest, IncidentResponse> updateIncidentHandler) {
            this.updateIncidentHandler = updateIncidentHandler;
        }

        void setAcknowledgeIncidentHandler(BiFunction<Long, IncidentActionRequest, IncidentResponse> acknowledgeIncidentHandler) {
            this.acknowledgeIncidentHandler = acknowledgeIncidentHandler;
        }

        void setCloseIncidentHandler(BiFunction<Long, IncidentActionRequest, IncidentResponse> closeIncidentHandler) {
            this.closeIncidentHandler = closeIncidentHandler;
        }

        @Override
        public IncidentResponse createIncident(IncidentCreateRequest request) {
            return createIncidentHandler.apply(request);
        }

        @Override
        public IncidentResponse getIncidentById(Long id) {
            return getIncidentByIdHandler.apply(id);
        }

        @Override
        public IncidentResponse updateIncident(Long id, IncidentUpdateRequest request) {
            return updateIncidentHandler.apply(id, request);
        }

        @Override
        public Page<IncidentResponse> getAllIncidents(
                int page,
                int size,
                String sortBy,
                String direction,
                IncidentPriority priority,
                List<String> priorities,
                String region,
                Boolean possiblyPlanned,
                IncidentStatus status,
                List<String> statuses,
                String incidentNumber,
                String title,
                String sourceAlarmType,
                LocalDateTime openedFrom,
                LocalDateTime openedTo,
                LocalDateTime acknowledgedFrom,
                LocalDateTime acknowledgedTo,
                LocalDateTime resolvedFrom,
                LocalDateTime resolvedTo,
                LocalDateTime closedFrom,
                LocalDateTime closedTo
        ) {
            return new PageImpl<>(List.of());
        }

        @Override
        public IncidentResponse acknowledgeIncident(Long id, IncidentActionRequest request) {
            return acknowledgeIncidentHandler.apply(id, request);
        }

        @Override
        public IncidentResponse closeIncident(Long id, IncidentActionRequest request) {
            return closeIncidentHandler.apply(id, request);
        }

        @Override
        public IncidentResponse resolveIncident(Long id, IncidentActionRequest request) {
            return new IncidentResponse();
        }

        @Override
        public List<IncidentTimelineResponse> getIncidentTimeline(Long id) {
            return List.of();
        }
    }
}
