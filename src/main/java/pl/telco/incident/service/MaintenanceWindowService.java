package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.entity.enums.MaintenanceStatus;
import pl.telco.incident.exception.BadRequestException;
import pl.telco.incident.exception.ResourceNotFoundException;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaintenanceWindowService {

    private final JdbcTemplate jdbcTemplate;
    private final NetworkNodeRepository networkNodeRepository;
    private final ObservabilityEventLogger observabilityEventLogger;

    @Transactional
    public MaintenanceWindowResponse createMaintenanceWindow(MaintenanceWindowCreateRequest request) {
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Long maintenanceWindowId = jdbcTemplate.queryForObject(
                """
                INSERT INTO maintenance_window (title, description, status, start_time, end_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                request.getTitle().trim(),
                request.getDescription(),
                request.getStatus().name(),
                Timestamp.valueOf(request.getStartTime()),
                Timestamp.valueOf(request.getEndTime()),
                Timestamp.valueOf(now)
        );

        logPersistenceEvent(
                "MaintenanceWindow",
                "maintenance_window",
                maintenanceWindowId,
                Map.of(
                        "title", request.getTitle().trim(),
                        "maintenanceStatus", request.getStatus(),
                        "startTime", request.getStartTime(),
                        "endTime", request.getEndTime()
                )
        );

        List<Long> nodeIds = new ArrayList<>();
        for (Long nodeId : deduplicateNodeIds(request.getNodeIds())) {
            Long maintenanceNodeId = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO maintenance_node (maintenance_window_id, network_node_id, created_at)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """,
                    Long.class,
                    maintenanceWindowId,
                    nodeId,
                    Timestamp.valueOf(now)
            );

            logPersistenceEvent(
                    "MaintenanceNode",
                    "maintenance_node",
                    maintenanceNodeId,
                    Map.of(
                            "maintenanceWindowId", maintenanceWindowId,
                            "networkNodeId", nodeId
                    )
            );
            nodeIds.add(nodeId);
        }

        MaintenanceWindowResponse response = new MaintenanceWindowResponse();
        response.setId(maintenanceWindowId);
        response.setTitle(request.getTitle().trim());
        response.setDescription(request.getDescription());
        response.setStatus(request.getStatus());
        response.setStartTime(request.getStartTime());
        response.setEndTime(request.getEndTime());
        response.setNodeIds(nodeIds);
        return response;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> getMaintenanceWindows() {
        List<MaintenanceWindowResponse> responses = jdbcTemplate.query(
                """
                SELECT id, title, description, status, start_time, end_time
                FROM maintenance_window
                ORDER BY start_time DESC
                """,
                (rs, rowNum) -> {
                    MaintenanceWindowResponse response = new MaintenanceWindowResponse();
                    response.setId(rs.getLong("id"));
                    response.setTitle(rs.getString("title"));
                    response.setDescription(rs.getString("description"));
                    response.setStatus(MaintenanceStatus.valueOf(rs.getString("status")));
                    response.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    response.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    response.setNodeIds(new ArrayList<>());
                    return response;
                }
        );

        Map<Long, List<Long>> nodeIdsByWindowId = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT maintenance_window_id, network_node_id
                FROM maintenance_node
                ORDER BY maintenance_window_id, id
                """,
                (ResultSetExtractor<Void>) rs -> {
                    while (rs.next()) {
                        nodeIdsByWindowId
                                .computeIfAbsent(rs.getLong("maintenance_window_id"), ignored -> new ArrayList<>())
                                .add(rs.getLong("network_node_id"));
                    }
                    return null;
                }
        );

        for (MaintenanceWindowResponse response : responses) {
            response.setNodeIds(nodeIdsByWindowId.getOrDefault(response.getId(), List.of()));
        }

        return responses;
    }

    private void validateRequest(MaintenanceWindowCreateRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be later than startTime");
        }

        List<Long> uniqueNodeIds = deduplicateNodeIds(request.getNodeIds());
        List<Long> existingNodeIds = networkNodeRepository.findAllById(uniqueNodeIds).stream()
                .map(node -> node.getId())
                .toList();

        if (existingNodeIds.size() != uniqueNodeIds.size()) {
            throw new ResourceNotFoundException("One or more network nodes were not found");
        }
    }

    private List<Long> deduplicateNodeIds(List<Long> nodeIds) {
        return new ArrayList<>(new LinkedHashSet<>(nodeIds));
    }

    private void logPersistenceEvent(String entityType, String tableName, Long entityId, Map<String, Object> additionalFields) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityType", entityType);
        fields.put("tableName", tableName);
        fields.put("entityId", entityId);
        fields.putAll(additionalFields);

        observabilityEventLogger.logEvent(
                "maintenance",
                "persistence",
                "insert",
                "entity_change",
                fields
        );
    }
}
