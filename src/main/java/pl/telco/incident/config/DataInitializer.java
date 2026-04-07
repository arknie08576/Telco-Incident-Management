package pl.telco.incident.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;
import pl.telco.incident.entity.enums.MaintenanceStatus;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;
import pl.telco.incident.observability.ObservabilityEventLogger;
import pl.telco.incident.repository.AlarmEventRepository;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.IncidentTimelineRepository;
import pl.telco.incident.repository.MaintenanceWindowRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final ObservabilityEventLogger observabilityEventLogger;

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
    public CommandLineRunner initData() {
        return args -> {
            logSystemSeedEvent("start", "seed_start", Map.of("seedEnabled", true));
            seedNetworkNodesIfEmpty();
            seedIncidentsIfEmpty();
            seedMaintenanceWindowsIfEmpty();
            seedAlarmEventsIfEmpty();
            seedIncidentTimelineIfEmpty();
            logSystemSeedEvent("complete", "seed_complete", Map.of("seedEnabled", true));
        };
    }

    private void seedNetworkNodesIfEmpty() {
        long count = networkNodeRepository.count();
        if (count > 0) {
            logSeedSkip("network_node", count);
            return;
        }

        NetworkNode node1 = NetworkNode.builder()
                .nodeName("CORE-RTR-WAW-01")
                .nodeType(NodeType.ROUTER)
                .region(Region.MAZOWIECKIE)
                .vendor("Cisco")
                .active(true)
                .build();

        NetworkNode node2 = NetworkNode.builder()
                .nodeName("RAN-GNB-WAW-01")
                .nodeType(NodeType.G_NODE_B)
                .region(Region.MAZOWIECKIE)
                .vendor("Ericsson")
                .active(true)
                .build();

        NetworkNode node3 = NetworkNode.builder()
                .nodeName("RAN-GNB-WAW-02")
                .nodeType(NodeType.G_NODE_B)
                .region(Region.MAZOWIECKIE)
                .vendor("Nokia")
                .active(true)
                .build();

        NetworkNode node4 = NetworkNode.builder()
                .nodeName("RAN-ENB-WAW-01")
                .nodeType(NodeType.E_NODE_B)
                .region(Region.MALOPOLSKIE)
                .vendor("Huawei")
                .active(true)
                .build();

        NetworkNode node5 = NetworkNode.builder()
                .nodeName("CORE-SBC-WAW-01")
                .nodeType(NodeType.SBC)
                .region(Region.SLASKIE)
                .vendor("Oracle")
                .active(true)
                .build();

        networkNodeRepository.saveAll(List.of(node1, node2, node3, node4, node5));
        networkNodeRepository.flush();
        logSystemSeedEvent("seed", "network_node_seeded", Map.of("tableName", "network_node", "rowCount", 5));
    }

    private void seedIncidentsIfEmpty() {
        long count = incidentRepository.count();
        if (count > 0) {
            logSeedSkip("incident", count);
            return;
        }

        NetworkNode router = getNodeByName("CORE-RTR-WAW-01");
        NetworkNode gnb1 = getNodeByName("RAN-GNB-WAW-01");
        NetworkNode gnb2 = getNodeByName("RAN-GNB-WAW-02");
        NetworkNode enb = getNodeByName("RAN-ENB-WAW-01");
        NetworkNode sbc = getNodeByName("CORE-SBC-WAW-01");

        LocalDateTime now = LocalDateTime.now();

        Incident inc1 = new Incident();
        inc1.setIncidentNumber("INC-001");
        inc1.setTitle("Router failure in Warsaw");
        inc1.setStatus(IncidentStatus.OPEN);
        inc1.setPriority(IncidentPriority.HIGH);
        inc1.setRegion(Region.MAZOWIECKIE);
        inc1.setSourceAlarmType(SourceAlarmType.HARDWARE);
        inc1.setPossiblyPlanned(false);
        inc1.setRootNode(router);
        inc1.setOpenedAt(now.minusHours(2));
        inc1.setCreatedAt(now.minusHours(2));
        inc1.setUpdatedAt(now.minusHours(2));
        inc1.addIncidentNode(createIncidentNode(router, IncidentNodeRole.ROOT));
        inc1.addIncidentNode(createIncidentNode(gnb1, IncidentNodeRole.AFFECTED));
        inc1.addIncidentNode(createIncidentNode(gnb2, IncidentNodeRole.AFFECTED));

        Incident inc2 = new Incident();
        inc2.setIncidentNumber("INC-002");
        inc2.setTitle("5G cell degradation");
        inc2.setStatus(IncidentStatus.ACKNOWLEDGED);
        inc2.setPriority(IncidentPriority.MEDIUM);
        inc2.setRegion(Region.MAZOWIECKIE);
        inc2.setSourceAlarmType(SourceAlarmType.PERFORMANCE);
        inc2.setPossiblyPlanned(false);
        inc2.setRootNode(gnb1);
        inc2.setOpenedAt(now.minusHours(6));
        inc2.setAcknowledgedAt(now.minusHours(5));
        inc2.setCreatedAt(now.minusHours(6));
        inc2.setUpdatedAt(now.minusHours(5));
        inc2.addIncidentNode(createIncidentNode(gnb1, IncidentNodeRole.ROOT));
        inc2.addIncidentNode(createIncidentNode(gnb2, IncidentNodeRole.AFFECTED));

        Incident inc3 = new Incident();
        inc3.setIncidentNumber("INC-003");
        inc3.setTitle("SBC overload");
        inc3.setStatus(IncidentStatus.RESOLVED);
        inc3.setPriority(IncidentPriority.CRITICAL);
        inc3.setRegion(Region.SLASKIE);
        inc3.setSourceAlarmType(SourceAlarmType.CAPACITY);
        inc3.setPossiblyPlanned(false);
        inc3.setRootNode(sbc);
        inc3.setOpenedAt(now.minusDays(1));
        inc3.setAcknowledgedAt(now.minusHours(20));
        inc3.setResolvedAt(now.minusHours(4));
        inc3.setCreatedAt(now.minusDays(1));
        inc3.setUpdatedAt(now.minusHours(4));
        inc3.addIncidentNode(createIncidentNode(sbc, IncidentNodeRole.ROOT));
        inc3.addIncidentNode(createIncidentNode(router, IncidentNodeRole.AFFECTED));
        inc3.addIncidentNode(createIncidentNode(gnb1, IncidentNodeRole.AFFECTED));

        Incident inc4 = new Incident();
        inc4.setIncidentNumber("INC-004");
        inc4.setTitle("Planned LTE maintenance");
        inc4.setStatus(IncidentStatus.OPEN);
        inc4.setPriority(IncidentPriority.LOW);
        inc4.setRegion(Region.MALOPOLSKIE);
        inc4.setSourceAlarmType(SourceAlarmType.MAINTENANCE);
        inc4.setPossiblyPlanned(true);
        inc4.setRootNode(enb);
        inc4.setOpenedAt(now.minusDays(2));
        inc4.setCreatedAt(now.minusDays(2));
        inc4.setUpdatedAt(now.minusDays(2));
        inc4.addIncidentNode(createIncidentNode(enb, IncidentNodeRole.ROOT));

        Incident inc5 = new Incident();
        inc5.setIncidentNumber("INC-005");
        inc5.setTitle("Packet loss issue");
        inc5.setStatus(IncidentStatus.CLOSED);
        inc5.setPriority(IncidentPriority.HIGH);
        inc5.setRegion(Region.MAZOWIECKIE);
        inc5.setSourceAlarmType(SourceAlarmType.NETWORK);
        inc5.setPossiblyPlanned(false);
        inc5.setRootNode(gnb2);
        inc5.setOpenedAt(now.minusDays(5));
        inc5.setAcknowledgedAt(now.minusDays(5).plusHours(1));
        inc5.setResolvedAt(now.minusDays(4));
        inc5.setClosedAt(now.minusDays(4).plusHours(4));
        inc5.setCreatedAt(now.minusDays(5));
        inc5.setUpdatedAt(now.minusDays(4).plusHours(4));
        inc5.addIncidentNode(createIncidentNode(gnb2, IncidentNodeRole.ROOT));
        inc5.addIncidentNode(createIncidentNode(gnb1, IncidentNodeRole.AFFECTED));

        Incident inc6 = new Incident();
        inc6.setIncidentNumber("INC-006");
        inc6.setTitle("gNodeB reboot loop");
        inc6.setStatus(IncidentStatus.OPEN);
        inc6.setPriority(IncidentPriority.CRITICAL);
        inc6.setRegion(Region.POMORSKIE);
        inc6.setSourceAlarmType(SourceAlarmType.HARDWARE);
        inc6.setPossiblyPlanned(false);
        inc6.setRootNode(gnb1);
        inc6.setOpenedAt(now.minusHours(10));
        inc6.setCreatedAt(now.minusHours(10));
        inc6.setUpdatedAt(now.minusHours(10));
        inc6.addIncidentNode(createIncidentNode(gnb1, IncidentNodeRole.ROOT));
        inc6.addIncidentNode(createIncidentNode(enb, IncidentNodeRole.AFFECTED));
        inc6.addIncidentNode(createIncidentNode(sbc, IncidentNodeRole.AFFECTED));

        incidentRepository.saveAll(List.of(inc1, inc2, inc3, inc4, inc5, inc6));
        incidentRepository.flush();
        logSystemSeedEvent("seed", "incident_seeded", Map.of("tableName", "incident", "rowCount", 6));
    }

    private void seedMaintenanceWindowsIfEmpty() {
        long count = maintenanceWindowRepository.count();
        if (count > 0) {
            logSeedSkip("maintenance_window", count);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        MaintenanceWindow firstWindow = new MaintenanceWindow();
        firstWindow.setTitle("Planned RAN upgrade - Krakow");
        firstWindow.setDescription("Software upgrade for LTE/5G access nodes");
        firstWindow.setStatus(MaintenanceStatus.PLANNED);
        firstWindow.setStartTime(now.plusDays(1));
        firstWindow.setEndTime(now.plusDays(1).plusHours(4));
        firstWindow.setCreatedAt(now);
        firstWindow.addMaintenanceNode(createMaintenanceNode(getNodeByName("RAN-ENB-WAW-01"), now));
        firstWindow.addMaintenanceNode(createMaintenanceNode(getNodeByName("RAN-GNB-WAW-01"), now));

        MaintenanceWindow secondWindow = new MaintenanceWindow();
        secondWindow.setTitle("Core SBC patching");
        secondWindow.setDescription("Security patch deployment on SBC layer");
        secondWindow.setStatus(MaintenanceStatus.COMPLETED);
        secondWindow.setStartTime(now.minusDays(3));
        secondWindow.setEndTime(now.minusDays(3).plusHours(2));
        secondWindow.setCreatedAt(now.minusDays(4));
        secondWindow.addMaintenanceNode(createMaintenanceNode(getNodeByName("CORE-SBC-WAW-01"), now.minusDays(4)));

        maintenanceWindowRepository.saveAll(List.of(firstWindow, secondWindow));
        maintenanceWindowRepository.flush();

        logDatasetSeedEvent(
                "maintenance",
                "maintenance_window",
                "insert",
                Map.of("title", firstWindow.getTitle(), "maintenanceStatus", firstWindow.getStatus())
        );
        firstWindow.getMaintenanceNodes().forEach(node -> logDatasetSeedEvent(
                "maintenance",
                "maintenance_node",
                "insert",
                Map.of("maintenanceWindowId", firstWindow.getId(), "networkNodeId", node.getNetworkNode().getId())
        ));

        logDatasetSeedEvent(
                "maintenance",
                "maintenance_window",
                "insert",
                Map.of("title", secondWindow.getTitle(), "maintenanceStatus", secondWindow.getStatus())
        );
        secondWindow.getMaintenanceNodes().forEach(node -> logDatasetSeedEvent(
                "maintenance",
                "maintenance_node",
                "insert",
                Map.of("maintenanceWindowId", secondWindow.getId(), "networkNodeId", node.getNetworkNode().getId())
        ));
    }

    private void seedAlarmEventsIfEmpty() {
        long count = alarmEventRepository.count();
        if (count > 0) {
            logSeedSkip("alarm_event", count);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        AlarmEvent alarm1 = createAlarmEvent(
                "OSS",
                "ALARM-001",
                getNodeByName("CORE-RTR-WAW-01"),
                getIncidentByNumber("INC-001"),
                "LINK_DOWN",
                AlarmSeverity.MAJOR,
                AlarmStatus.OPEN,
                "Core router uplink down",
                false,
                now.minusHours(2),
                now.minusHours(2).plusMinutes(1),
                now.minusHours(2)
        );

        AlarmEvent alarm2 = createAlarmEvent(
                "OSS",
                "ALARM-002",
                getNodeByName("RAN-GNB-WAW-01"),
                getIncidentByNumber("INC-002"),
                "CELL_DEGRADED",
                AlarmSeverity.MINOR,
                AlarmStatus.ACKNOWLEDGED,
                "Degradation detected on 5G cell",
                false,
                now.minusHours(6),
                now.minusHours(6).plusMinutes(2),
                now.minusHours(6)
        );

        AlarmEvent alarm3 = createAlarmEvent(
                "EMS",
                "ALARM-003",
                getNodeByName("CORE-SBC-WAW-01"),
                getIncidentByNumber("INC-003"),
                "CPU_HIGH",
                AlarmSeverity.CRITICAL,
                AlarmStatus.CLEARED,
                "SBC CPU overload",
                false,
                now.minusDays(1),
                now.minusDays(1).plusMinutes(1),
                now.minusDays(1)
        );

        AlarmEvent alarm4 = createAlarmEvent(
                "OSS",
                "ALARM-004",
                getNodeByName("RAN-ENB-WAW-01"),
                getIncidentByNumber("INC-004"),
                "MAINTENANCE_MODE",
                AlarmSeverity.INFO,
                AlarmStatus.OPEN,
                "Node in maintenance mode",
                true,
                now.minusDays(2),
                now.minusDays(2).plusMinutes(1),
                now.minusDays(2)
        );

        AlarmEvent alarm5 = createAlarmEvent(
                "EMS",
                "ALARM-005",
                getNodeByName("RAN-GNB-WAW-02"),
                null,
                "PACKET_LOSS",
                AlarmSeverity.MAJOR,
                AlarmStatus.OPEN,
                "Packet loss detected but not yet correlated",
                false,
                now.minusHours(3),
                now.minusHours(3).plusMinutes(1),
                now.minusHours(3)
        );

        AlarmEvent alarm6 = createAlarmEvent(
                "OSS",
                "ALARM-006",
                getNodeByName("RAN-GNB-WAW-01"),
                getIncidentByNumber("INC-006"),
                "REBOOT_LOOP",
                AlarmSeverity.CRITICAL,
                AlarmStatus.OPEN,
                "Repeated reboot loop on gNodeB",
                false,
                now.minusHours(10),
                now.minusHours(10).plusMinutes(1),
                now.minusHours(10)
        );

        alarmEventRepository.saveAll(List.of(alarm1, alarm2, alarm3, alarm4, alarm5, alarm6));
        alarmEventRepository.flush();

        logAlarmSeedEvent(alarm1);
        logAlarmSeedEvent(alarm2);
        logAlarmSeedEvent(alarm3);
        logAlarmSeedEvent(alarm4);
        logAlarmSeedEvent(alarm5);
        logAlarmSeedEvent(alarm6);
    }

    private void seedIncidentTimelineIfEmpty() {
        long count = incidentTimelineRepository.count();
        if (count > 0) {
            logSeedSkip("incident_timeline", count);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        IncidentTimeline timeline1 = createTimelineEntry(
                getIncidentByNumber("INC-001"),
                IncidentTimelineEventType.CREATED,
                "Incident created from router failure alarm",
                now.minusHours(2)
        );
        IncidentTimeline timeline2 = createTimelineEntry(
                getIncidentByNumber("INC-002"),
                IncidentTimelineEventType.ACKNOWLEDGED,
                "NOC engineer acknowledged cell degradation",
                now.minusHours(5)
        );
        IncidentTimeline timeline3 = createTimelineEntry(
                getIncidentByNumber("INC-003"),
                IncidentTimelineEventType.RESOLVED,
                "Traffic rebalanced and SBC CPU normalized",
                now.minusHours(4)
        );
        IncidentTimeline timeline4 = createTimelineEntry(
                getIncidentByNumber("INC-005"),
                IncidentTimelineEventType.CLOSED,
                "Packet loss issue closed after verification",
                now.minusDays(4)
        );
        IncidentTimeline timeline5 = createTimelineEntry(
                getIncidentByNumber("INC-006"),
                IncidentTimelineEventType.CREATED,
                "Incident opened due to repeated reboot loop",
                now.minusHours(10)
        );

        incidentTimelineRepository.saveAll(List.of(timeline1, timeline2, timeline3, timeline4, timeline5));
        incidentTimelineRepository.flush();

        logTimelineSeedEvent(timeline1);
        logTimelineSeedEvent(timeline2);
        logTimelineSeedEvent(timeline3);
        logTimelineSeedEvent(timeline4);
        logTimelineSeedEvent(timeline5);
    }

    private void logSeedSkip(String tableName, long existingRowCount) {
        logSystemSeedEvent(
                "skip",
                "seed_skipped",
                Map.of(
                        "tableName", tableName,
                        "existingRowCount", existingRowCount
                )
        );
    }

    private void logSystemSeedEvent(String action, String message, Map<String, Object> fields) {
        observabilityEventLogger.logEvent(
                "system",
                "seed",
                action,
                message,
                fields
        );
    }

    private void logDatasetSeedEvent(String dataset, String tableName, String action, Map<String, Object> fields) {
        Map<String, Object> logFields = new LinkedHashMap<>();
        logFields.put("tableName", tableName);
        logFields.putAll(fields);

        observabilityEventLogger.logEvent(
                dataset,
                "seed",
                action,
                "seed_row_written",
                logFields
        );
    }

    private IncidentNode createIncidentNode(NetworkNode node, IncidentNodeRole role) {
        IncidentNode incidentNode = new IncidentNode();
        incidentNode.setNetworkNode(node);
        incidentNode.setRole(role);
        return incidentNode;
    }

    private MaintenanceNode createMaintenanceNode(NetworkNode node, LocalDateTime createdAt) {
        MaintenanceNode maintenanceNode = new MaintenanceNode();
        maintenanceNode.setNetworkNode(node);
        maintenanceNode.setCreatedAt(createdAt);
        return maintenanceNode;
    }

    private AlarmEvent createAlarmEvent(
            String sourceSystem,
            String externalId,
            NetworkNode networkNode,
            Incident incident,
            String alarmType,
            AlarmSeverity severity,
            AlarmStatus status,
            String description,
            boolean suppressedByMaintenance,
            LocalDateTime occurredAt,
            LocalDateTime receivedAt,
            LocalDateTime createdAt
    ) {
        AlarmEvent alarmEvent = new AlarmEvent();
        alarmEvent.setSourceSystem(sourceSystem);
        alarmEvent.setExternalId(externalId);
        alarmEvent.setNetworkNode(networkNode);
        alarmEvent.setIncident(incident);
        alarmEvent.setAlarmType(alarmType);
        alarmEvent.setSeverity(severity);
        alarmEvent.setStatus(status);
        alarmEvent.setDescription(description);
        alarmEvent.setSuppressedByMaintenance(suppressedByMaintenance);
        alarmEvent.setOccurredAt(occurredAt);
        alarmEvent.setReceivedAt(receivedAt);
        alarmEvent.setCreatedAt(createdAt);
        return alarmEvent;
    }

    private IncidentTimeline createTimelineEntry(
            Incident incident,
            IncidentTimelineEventType eventType,
            String message,
            LocalDateTime createdAt
    ) {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(eventType);
        timeline.setMessage(message);
        timeline.setCreatedAt(createdAt);
        return timeline;
    }

    private void logAlarmSeedEvent(AlarmEvent alarmEvent) {
        logDatasetSeedEvent(
                "alarm",
                "alarm_event",
                "insert",
                nullableMap(
                        "externalId", alarmEvent.getExternalId(),
                        "alarmType", alarmEvent.getAlarmType(),
                        "incidentId", alarmEvent.getIncident() != null ? alarmEvent.getIncident().getId() : null,
                        "networkNodeId", alarmEvent.getNetworkNode().getId()
                )
        );
    }

    private void logTimelineSeedEvent(IncidentTimeline timeline) {
        logDatasetSeedEvent(
                "incident",
                "incident_timeline",
                "insert",
                Map.of(
                        "incidentId", timeline.getIncident().getId(),
                        "timelineEventType", timeline.getEventType()
                )
        );
    }

    private Map<String, Object> nullableMap(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            fields.put((String) keyValues[i], keyValues[i + 1]);
        }

        return fields;
    }

    private NetworkNode getNodeByName(String nodeName) {
        return networkNodeRepository.findByNodeName(nodeName)
                .orElseThrow(() -> new IllegalStateException("Network node not found: " + nodeName));
    }

    private Incident getIncidentByNumber(String incidentNumber) {
        return incidentRepository.findByIncidentNumber(incidentNumber)
                .orElseThrow(() -> new IllegalStateException("Incident not found: " + incidentNumber));
    }
}
