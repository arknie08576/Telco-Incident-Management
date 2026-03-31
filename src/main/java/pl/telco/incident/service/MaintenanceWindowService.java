package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.telco.incident.dto.MaintenanceWindowCreateRequest;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.dto.MaintenanceWindowUpdateRequest;
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
                "insert",
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
                    "insert",
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

    @Transactional
    public MaintenanceWindowResponse updateMaintenanceWindow(Long id, MaintenanceWindowUpdateRequest request) {
        MaintenanceWindowResponse current = getMaintenanceWindowOrThrow(id);

        String title = current.getTitle();
        String description = current.getDescription();
        MaintenanceStatus status = current.getStatus();
        LocalDateTime startTime = current.getStartTime();
        LocalDateTime endTime = current.getEndTime();
        List<Long> nodeIds = new ArrayList<>(current.getNodeIds());
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null) {
            String normalizedTitle = request.getTitle().trim();
            if (!normalizedTitle.equals(title)) {
                title = normalizedTitle;
                changedFields.add("title");
            }
        }

        if (request.getDescription() != null && !request.getDescription().equals(description)) {
            description = request.getDescription();
            changedFields.add("description");
        }

        if (request.getStatus() != null && request.getStatus() != status) {
            status = request.getStatus();
            changedFields.add("status");
        }

        if (request.getStartTime() != null && !request.getStartTime().equals(startTime)) {
            startTime = request.getStartTime();
            changedFields.add("startTime");
        }

        if (request.getEndTime() != null && !request.getEndTime().equals(endTime)) {
            endTime = request.getEndTime();
            changedFields.add("endTime");
        }

        if (request.getNodeIds() != null) {
            List<Long> deduplicatedNodeIds = deduplicateNodeIds(request.getNodeIds());
            validateNodeIdsExist(deduplicatedNodeIds);

            if (!new LinkedHashSet<>(nodeIds).equals(new LinkedHashSet<>(deduplicatedNodeIds))) {
                nodeIds = deduplicatedNodeIds;
                changedFields.add("nodeIds");
            }
        }

        validateTimeRange(startTime, endTime);

        if (changedFields.isEmpty()) {
            throw new BadRequestException("Patch request does not change maintenance window");
        }

        if (changedFields.stream().anyMatch(field -> !"nodeIds".equals(field))) {
            jdbcTemplate.update(
                    """
                    UPDATE maintenance_window
                    SET title = ?, description = ?, status = ?, start_time = ?, end_time = ?
                    WHERE id = ?
                    """,
                    title,
                    description,
                    status.name(),
                    Timestamp.valueOf(startTime),
                    Timestamp.valueOf(endTime),
                    id
            );

            logPersistenceEvent(
                    "MaintenanceWindow",
                    "maintenance_window",
                    id,
                    "update",
                    Map.of(
                            "title", title,
                            "maintenanceStatus", status,
                            "startTime", startTime,
                            "endTime", endTime,
                            "changedFields", changedFields
                    )
            );
        }

        if (changedFields.contains("nodeIds")) {
            syncMaintenanceNodes(id, current.getNodeIds(), nodeIds);
        }

        return getMaintenanceWindowOrThrow(id);
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

    @Transactional(readOnly = true)
    public MaintenanceWindowResponse getMaintenanceWindowOrThrow(Long id) {
        MaintenanceWindowResponse response = jdbcTemplate.query(
                """
                SELECT id, title, description, status, start_time, end_time
                FROM maintenance_window
                WHERE id = ?
                """,
                (ResultSetExtractor<MaintenanceWindowResponse>) rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    MaintenanceWindowResponse found = new MaintenanceWindowResponse();
                    found.setId(rs.getLong("id"));
                    found.setTitle(rs.getString("title"));
                    found.setDescription(rs.getString("description"));
                    found.setStatus(MaintenanceStatus.valueOf(rs.getString("status")));
                    found.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    found.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    found.setNodeIds(new ArrayList<>());
                    return found;
                },
                id
        );

        if (response == null) {
            throw new ResourceNotFoundException("Maintenance window not found: " + id);
        }

        response.setNodeIds(loadNodeIdsByWindowId(id));
        return response;
    }

    private void validateRequest(MaintenanceWindowCreateRequest request) {
        List<Long> uniqueNodeIds = deduplicateNodeIds(request.getNodeIds());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateNodeIdsExist(uniqueNodeIds);
    }

    private List<Long> deduplicateNodeIds(List<Long> nodeIds) {
        return new ArrayList<>(new LinkedHashSet<>(nodeIds));
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be later than startTime");
        }
    }

    private void validateNodeIdsExist(List<Long> nodeIds) {
        List<Long> existingNodeIds = networkNodeRepository.findAllById(nodeIds).stream()
                .map(node -> node.getId())
                .toList();

        if (existingNodeIds.size() != nodeIds.size()) {
            throw new ResourceNotFoundException("One or more network nodes were not found");
        }
    }

    private List<Long> loadNodeIdsByWindowId(Long maintenanceWindowId) {
        return jdbcTemplate.query(
                """
                SELECT network_node_id
                FROM maintenance_node
                WHERE maintenance_window_id = ?
                ORDER BY id
                """,
                (rs, rowNum) -> rs.getLong("network_node_id"),
                maintenanceWindowId
        );
    }

    private void syncMaintenanceNodes(Long maintenanceWindowId, List<Long> currentNodeIds, List<Long> requestedNodeIds) {
        LinkedHashSet<Long> current = new LinkedHashSet<>(currentNodeIds);
        LinkedHashSet<Long> requested = new LinkedHashSet<>(requestedNodeIds);

        for (Long nodeId : current) {
            if (!requested.contains(nodeId)) {
                jdbcTemplate.update(
                        "DELETE FROM maintenance_node WHERE maintenance_window_id = ? AND network_node_id = ?",
                        maintenanceWindowId,
                        nodeId
                );
                logPersistenceEvent(
                        "MaintenanceNode",
                        "maintenance_node",
                        null,
                        "delete",
                        Map.of(
                                "maintenanceWindowId", maintenanceWindowId,
                                "networkNodeId", nodeId
                        )
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long nodeId : requested) {
            if (!current.contains(nodeId)) {
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
                        "insert",
                        Map.of(
                                "maintenanceWindowId", maintenanceWindowId,
                                "networkNodeId", nodeId
                        )
                );
            }
        }
    }

    private void logPersistenceEvent(String entityType, String tableName, Long entityId, String action, Map<String, Object> additionalFields) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entityType", entityType);
        fields.put("tableName", tableName);
        fields.put("entityId", entityId);
        fields.putAll(additionalFields);

        observabilityEventLogger.logEvent(
                "maintenance",
                "persistence",
                action,
                "entity_change",
                fields
        );
    }
}
