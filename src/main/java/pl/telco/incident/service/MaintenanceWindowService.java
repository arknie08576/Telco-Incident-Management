package pl.telco.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MaintenanceWindowService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "mw.id",
            "title", "mw.title",
            "status", "mw.status",
            "startTime", "mw.start_time",
            "endTime", "mw.end_time"
    );

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
    public MaintenanceWindowResponse getMaintenanceWindowById(Long id) {
        return getMaintenanceWindowOrThrow(id);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceWindowResponse> getMaintenanceWindows(
            int page,
            int size,
            String sortBy,
            String direction,
            MaintenanceStatus status,
            List<String> statuses,
            String title,
            Long nodeId,
            LocalDateTime startFrom,
            LocalDateTime startTo,
            LocalDateTime endFrom,
            LocalDateTime endTo
    ) {
        validateSortBy(sortBy);
        validateDateRange("startFrom", startFrom, "startTo", startTo);
        validateDateRange("endFrom", endFrom, "endTo", endTo);

        Set<MaintenanceStatus> statusFilters = mergeStatusFilters(status, statuses);
        Sort.Direction sortDirection = parseSortDirection(direction);
        Pageable pageable = PageRequest.of(page, size);

        QueryFragments queryFragments = buildWhereClause(statusFilters, title, nodeId, startFrom, startTo, endFrom, endTo);

        String listSql = """
                SELECT mw.id, mw.title, mw.description, mw.status, mw.start_time, mw.end_time
                FROM maintenance_window mw
                """ + queryFragments.whereClause()
                + " ORDER BY " + ALLOWED_SORT_FIELDS.get(sortBy) + " " + sortDirection.name() + ", mw.id DESC"
                + " LIMIT ? OFFSET ?";

        List<Object> listParams = new ArrayList<>(queryFragments.params());
        listParams.add(size);
        listParams.add((long) page * size);

        List<MaintenanceWindowResponse> content = jdbcTemplate.query(
                listSql,
                (rs, rowNum) -> mapMaintenanceWindowRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()
                ),
                listParams.toArray()
        );

        Map<Long, List<Long>> nodeIdsByWindowId = loadNodeIdsByWindowIds(content.stream()
                .map(MaintenanceWindowResponse::getId)
                .toList());
        for (MaintenanceWindowResponse response : content) {
            response.setNodeIds(nodeIdsByWindowId.getOrDefault(response.getId(), List.of()));
        }

        Long totalElements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM maintenance_window mw " + queryFragments.whereClause(),
                Long.class,
                queryFragments.params().toArray()
        );

        return new PageImpl<>(content, pageable, totalElements == null ? 0 : totalElements);
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

                    return mapMaintenanceWindowRow(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("status"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime()
                    );
                },
                id
        );

        if (response == null) {
            throw new ResourceNotFoundException("Maintenance window not found: " + id);
        }

        response.setNodeIds(loadNodeIdsByWindowId(id));
        return response;
    }

    private QueryFragments buildWhereClause(
            Set<MaintenanceStatus> statusFilters,
            String title,
            Long nodeId,
            LocalDateTime startFrom,
            LocalDateTime startTo,
            LocalDateTime endFrom,
            LocalDateTime endTo
    ) {
        StringBuilder whereClause = new StringBuilder("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (!statusFilters.isEmpty()) {
            whereClause.append(" AND mw.status IN (")
                    .append(buildPlaceholders(statusFilters.size()))
                    .append(")");
            statusFilters.forEach(value -> params.add(value.name()));
        }

        if (title != null && !title.isBlank()) {
            whereClause.append(" AND LOWER(mw.title) LIKE ?");
            params.add("%" + title.trim().toLowerCase(Locale.ROOT) + "%");
        }

        if (nodeId != null) {
            whereClause.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM maintenance_node mn
                         WHERE mn.maintenance_window_id = mw.id
                           AND mn.network_node_id = ?
                     )
                    """);
            params.add(nodeId);
        }

        if (startFrom != null) {
            whereClause.append(" AND mw.start_time >= ?");
            params.add(Timestamp.valueOf(startFrom));
        }

        if (startTo != null) {
            whereClause.append(" AND mw.start_time <= ?");
            params.add(Timestamp.valueOf(startTo));
        }

        if (endFrom != null) {
            whereClause.append(" AND mw.end_time >= ?");
            params.add(Timestamp.valueOf(endFrom));
        }

        if (endTo != null) {
            whereClause.append(" AND mw.end_time <= ?");
            params.add(Timestamp.valueOf(endTo));
        }

        return new QueryFragments(whereClause.toString(), params);
    }

    private Map<Long, List<Long>> loadNodeIdsByWindowIds(List<Long> maintenanceWindowIds) {
        if (maintenanceWindowIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> nodeIdsByWindowId = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT maintenance_window_id, network_node_id
                FROM maintenance_node
                WHERE maintenance_window_id IN (%s)
                ORDER BY maintenance_window_id, id
                """.formatted(buildPlaceholders(maintenanceWindowIds.size())),
                (ResultSetExtractor<Void>) rs -> {
                    while (rs.next()) {
                        nodeIdsByWindowId
                                .computeIfAbsent(rs.getLong("maintenance_window_id"), ignored -> new ArrayList<>())
                                .add(rs.getLong("network_node_id"));
                    }
                    return null;
                },
                maintenanceWindowIds.toArray()
        );

        return nodeIdsByWindowId;
    }

    private MaintenanceWindowResponse mapMaintenanceWindowRow(
            Long id,
            String title,
            String description,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        MaintenanceWindowResponse response = new MaintenanceWindowResponse();
        response.setId(id);
        response.setTitle(title);
        response.setDescription(description);
        response.setStatus(MaintenanceStatus.valueOf(status));
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setNodeIds(new ArrayList<>());
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

    private void validateDateRange(String fromFieldName, LocalDateTime from, String toFieldName, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(fromFieldName + " must be earlier than or equal to " + toFieldName);
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

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.containsKey(sortBy)) {
            throw new BadRequestException("Unsupported sortBy value: " + sortBy);
        }
    }

    private Sort.Direction parseSortDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported direction value: " + direction);
        }
    }

    private Set<MaintenanceStatus> mergeStatusFilters(MaintenanceStatus status, List<String> statuses) {
        LinkedHashSet<MaintenanceStatus> merged = new LinkedHashSet<>();

        if (status != null) {
            merged.add(status);
        }
        merged.addAll(parseEnumFilters(statuses, MaintenanceStatus.class, "statuses"));

        return merged;
    }

    private <E extends Enum<E>> Set<E> parseEnumFilters(List<String> rawValues, Class<E> enumType, String parameterName) {
        LinkedHashSet<E> parsedValues = new LinkedHashSet<>();

        if (rawValues == null) {
            return parsedValues;
        }

        for (String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            for (String token : rawValue.split(",")) {
                String normalizedToken = token.trim();
                if (normalizedToken.isEmpty()) {
                    continue;
                }

                try {
                    parsedValues.add(Enum.valueOf(enumType, normalizedToken.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid value '%s' for parameter '%s'".formatted(normalizedToken, parameterName));
                }
            }
        }

        return parsedValues;
    }

    private String buildPlaceholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
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

    private record QueryFragments(String whereClause, List<Object> params) {
    }
}
