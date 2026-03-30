package pl.telco.incident.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.telco.incident.config.RequestCorrelationFilter;
import pl.telco.incident.dto.IncidentNodeCrudRequest;
import pl.telco.incident.dto.IncidentNodeCrudResponse;
import pl.telco.incident.dto.IncidentTimelineEntryRequest;
import pl.telco.incident.dto.IncidentTimelineEntryResponse;
import pl.telco.incident.dto.MaintenanceNodeRequest;
import pl.telco.incident.dto.MaintenanceNodeResponse;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ConflictException;
import pl.telco.incident.exception.GlobalExceptionHandler;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.service.IncidentNodeService;
import pl.telco.incident.service.IncidentTimelineEntryService;
import pl.telco.incident.service.MaintenanceNodeService;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        IncidentNodeController.class,
        MaintenanceNodeController.class,
        IncidentTimelineEntryController.class
})
@Import({
        GlobalExceptionHandler.class,
        RequestCorrelationFilter.class,
        ReferenceDataControllersWebMvcTest.TestConfig.class
})
class ReferenceDataControllersWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeIncidentNodeService incidentNodeService;

    @Autowired
    private FakeMaintenanceNodeService maintenanceNodeService;

    @Autowired
    private FakeIncidentTimelineEntryService incidentTimelineEntryService;

    @Test
    void createIncidentNodeShouldReturnValidationErrorsForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "incidentId": 0,
                  "networkNodeId": 0
                }
                """;

        mockMvc.perform(post("/api/incident-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.incidentId").value("incidentId must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.networkNodeId").value("networkNodeId must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.role").value("role is required"));
    }

    @Test
    void createIncidentNodeShouldReturnBadRequestForInvalidEnumInJsonBody() throws Exception {
        String requestBody = """
                {
                  "incidentId": 10,
                  "networkNodeId": 20,
                  "role": "BROKEN"
                }
                """;

        mockMvc.perform(post("/api/incident-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid enum value"));
    }

    @Test
    void updateIncidentNodeShouldMapConflictException() throws Exception {
        incidentNodeService.setUpdateHandler((id, request) -> {
            throw new ConflictException("Incident node relation already exists for incidentId/networkNodeId: 11/22");
        });

        mockMvc.perform(put("/api/incident-nodes/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentId": 11,
                                  "networkNodeId": 22,
                                  "role": "AFFECTED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incident node relation already exists for incidentId/networkNodeId: 11/22"));
    }

    @Test
    void deleteIncidentNodeShouldMapBadRequestException() throws Exception {
        incidentNodeService.setDeleteHandler(id -> {
            throw new BadRequestException("ROOT incident node cannot be deleted directly");
        });

        mockMvc.perform(delete("/api/incident-nodes/{id}", 7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ROOT incident node cannot be deleted directly"));
    }

    @Test
    void getMaintenanceNodeByIdShouldMapResourceNotFoundException() throws Exception {
        maintenanceNodeService.setGetByIdHandler(id -> {
            throw new ResourceNotFoundException("Maintenance node not found: " + id);
        });

        mockMvc.perform(get("/api/maintenance-nodes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Maintenance node not found: 99"));
    }

    @Test
    void createMaintenanceNodeShouldReturnValidationErrorsForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "maintenanceWindowId": 0,
                  "networkNodeId": 0
                }
                """;

        mockMvc.perform(post("/api/maintenance-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.maintenanceWindowId").value("maintenanceWindowId must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.networkNodeId").value("networkNodeId must be greater than 0"));
    }

    @Test
    void updateMaintenanceNodeShouldMapConflictException() throws Exception {
        maintenanceNodeService.setUpdateHandler((id, request) -> {
            throw new ConflictException("Maintenance node relation already exists for maintenanceWindowId/networkNodeId: 12/3");
        });

        mockMvc.perform(put("/api/maintenance-nodes/{id}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maintenanceWindowId": 12,
                                  "networkNodeId": 3
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Maintenance node relation already exists for maintenanceWindowId/networkNodeId: 12/3"));
    }

    @Test
    void createIncidentTimelineEntryShouldReturnValidationErrorsForInvalidBody() throws Exception {
        String requestBody = """
                {
                  "incidentId": 0,
                  "eventType": "",
                  "message": ""
                }
                """;

        mockMvc.perform(post("/api/incident-timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.incidentId").value("incidentId must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.eventType").value("eventType is required"))
                .andExpect(jsonPath("$.fieldErrors.message").value("message is required"));
    }

    @Test
    void createIncidentTimelineEntryShouldMapResourceNotFoundException() throws Exception {
        incidentTimelineEntryService.setCreateHandler(request -> {
            throw new ResourceNotFoundException("Incident not found: 404");
        });

        mockMvc.perform(post("/api/incident-timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentId": 404,
                                  "eventType": "MANUAL_NOTE",
                                  "message": "Operator note"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident not found: 404"));
    }

    @Test
    void getAllIncidentNodesShouldReturnGeneratedRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/incident-nodes"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER));
    }

    @Test
    void getAllIncidentNodesShouldPreserveIncomingRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/incident-nodes")
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-ref-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, "req-ref-123"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        FakeIncidentNodeService incidentNodeService() {
            return new FakeIncidentNodeService();
        }

        @Bean
        FakeMaintenanceNodeService maintenanceNodeService() {
            return new FakeMaintenanceNodeService();
        }

        @Bean
        FakeIncidentTimelineEntryService incidentTimelineEntryService() {
            return new FakeIncidentTimelineEntryService();
        }
    }

    static class FakeIncidentNodeService extends IncidentNodeService {

        private Function<IncidentNodeCrudRequest, IncidentNodeCrudResponse> createHandler =
                request -> new IncidentNodeCrudResponse();
        private BiFunction<Long, IncidentNodeCrudRequest, IncidentNodeCrudResponse> updateHandler =
                (id, request) -> new IncidentNodeCrudResponse();
        private LongFunction<IncidentNodeCrudResponse> getByIdHandler =
                id -> new IncidentNodeCrudResponse();
        private Consumer<Long> deleteHandler = id -> { };

        FakeIncidentNodeService() {
            super(null, null, null);
        }

        void setUpdateHandler(BiFunction<Long, IncidentNodeCrudRequest, IncidentNodeCrudResponse> updateHandler) {
            this.updateHandler = updateHandler;
        }

        void setDeleteHandler(Consumer<Long> deleteHandler) {
            this.deleteHandler = deleteHandler;
        }

        @Override
        public List<IncidentNodeCrudResponse> getAllIncidentNodes() {
            return List.of();
        }

        @Override
        public IncidentNodeCrudResponse getIncidentNodeById(Long id) {
            return getByIdHandler.apply(id);
        }

        @Override
        public IncidentNodeCrudResponse createIncidentNode(IncidentNodeCrudRequest request) {
            return createHandler.apply(request);
        }

        @Override
        public IncidentNodeCrudResponse updateIncidentNode(Long id, IncidentNodeCrudRequest request) {
            return updateHandler.apply(id, request);
        }

        @Override
        public void deleteIncidentNode(Long id) {
            deleteHandler.accept(id);
        }
    }

    static class FakeMaintenanceNodeService extends MaintenanceNodeService {

        private Function<MaintenanceNodeRequest, MaintenanceNodeResponse> createHandler =
                request -> new MaintenanceNodeResponse();
        private BiFunction<Long, MaintenanceNodeRequest, MaintenanceNodeResponse> updateHandler =
                (id, request) -> new MaintenanceNodeResponse();
        private LongFunction<MaintenanceNodeResponse> getByIdHandler =
                id -> new MaintenanceNodeResponse();
        private Consumer<Long> deleteHandler = id -> { };

        FakeMaintenanceNodeService() {
            super(null, null, null);
        }

        void setGetByIdHandler(LongFunction<MaintenanceNodeResponse> getByIdHandler) {
            this.getByIdHandler = getByIdHandler;
        }

        void setUpdateHandler(BiFunction<Long, MaintenanceNodeRequest, MaintenanceNodeResponse> updateHandler) {
            this.updateHandler = updateHandler;
        }

        @Override
        public List<MaintenanceNodeResponse> getAllMaintenanceNodes() {
            return List.of();
        }

        @Override
        public MaintenanceNodeResponse getMaintenanceNodeById(Long id) {
            return getByIdHandler.apply(id);
        }

        @Override
        public MaintenanceNodeResponse createMaintenanceNode(MaintenanceNodeRequest request) {
            return createHandler.apply(request);
        }

        @Override
        public MaintenanceNodeResponse updateMaintenanceNode(Long id, MaintenanceNodeRequest request) {
            return updateHandler.apply(id, request);
        }

        @Override
        public void deleteMaintenanceNode(Long id) {
            deleteHandler.accept(id);
        }
    }

    static class FakeIncidentTimelineEntryService extends IncidentTimelineEntryService {

        private Function<IncidentTimelineEntryRequest, IncidentTimelineEntryResponse> createHandler =
                request -> new IncidentTimelineEntryResponse();
        private BiFunction<Long, IncidentTimelineEntryRequest, IncidentTimelineEntryResponse> updateHandler =
                (id, request) -> new IncidentTimelineEntryResponse();
        private LongFunction<IncidentTimelineEntryResponse> getByIdHandler =
                id -> new IncidentTimelineEntryResponse();
        private Consumer<Long> deleteHandler = id -> { };

        FakeIncidentTimelineEntryService() {
            super(null, null);
        }

        void setCreateHandler(Function<IncidentTimelineEntryRequest, IncidentTimelineEntryResponse> createHandler) {
            this.createHandler = createHandler;
        }

        @Override
        public List<IncidentTimelineEntryResponse> getAllIncidentTimelineEntries() {
            return List.of();
        }

        @Override
        public IncidentTimelineEntryResponse getIncidentTimelineEntryById(Long id) {
            return getByIdHandler.apply(id);
        }

        @Override
        public IncidentTimelineEntryResponse createIncidentTimelineEntry(IncidentTimelineEntryRequest request) {
            return createHandler.apply(request);
        }

        @Override
        public IncidentTimelineEntryResponse updateIncidentTimelineEntry(Long id, IncidentTimelineEntryRequest request) {
            return updateHandler.apply(id, request);
        }

        @Override
        public void deleteIncidentTimelineEntry(Long id) {
            deleteHandler.accept(id);
        }
    }
}
