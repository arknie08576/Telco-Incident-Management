package pl.telco.incident.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.repository.IncidentRepository;
import pl.telco.incident.repository.NetworkNodeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final NetworkNodeRepository networkNodeRepository;
    private final IncidentRepository incidentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner initData() {
        return args -> {
            seedNetworkNodesIfEmpty();
            seedIncidentsIfEmpty();
            seedMaintenanceWindowsIfEmpty();
            seedAlarmEventsIfEmpty();
            seedIncidentTimelineIfEmpty();
        };
    }

    private void seedNetworkNodesIfEmpty() {
        if (networkNodeRepository.count() > 0) {
            return;
        }

        NetworkNode node1 = NetworkNode.builder()
                .nodeName("CORE-RTR-WAW-01")
                .nodeType(NodeType.ROUTER)
                .region("MAZOWIECKIE")
                .vendor("Cisco")
                .active(true)
                .build();

        NetworkNode node2 = NetworkNode.builder()
                .nodeName("RAN-GNB-WAW-01")
                .nodeType(NodeType.G_NODE_B)
                .region("MAZOWIECKIE")
                .vendor("Ericsson")
                .active(true)
                .build();

        NetworkNode node3 = NetworkNode.builder()
                .nodeName("RAN-GNB-WAW-02")
                .nodeType(NodeType.G_NODE_B)
                .region("MAZOWIECKIE")
                .vendor("Nokia")
                .active(true)
                .build();

        NetworkNode node4 = NetworkNode.builder()
                .nodeName("RAN-ENB-WAW-01")
                .nodeType(NodeType.E_NODE_B)
                .region("MALOPOLSKIE")
                .vendor("Huawei")
                .active(true)
                .build();

        NetworkNode node5 = NetworkNode.builder()
                .nodeName("CORE-SBC-WAW-01")
                .nodeType(NodeType.SBC)
                .region("SLASKIE")
                .vendor("Oracle")
                .active(true)
                .build();

        networkNodeRepository.saveAll(List.of(node1, node2, node3, node4, node5));
        networkNodeRepository.flush();
    }

    private void seedIncidentsIfEmpty() {
        if (incidentRepository.count() > 0) {
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
        inc1.setRegion("MAZOWIECKIE");
        inc1.setSourceAlarmType("HARDWARE");
        inc1.setPossiblyPlanned(false);
        inc1.setRootNode(router);
        inc1.setOpenedAt(now.minusHours(2));
        inc1.setCreatedAt(now.minusHours(2));
        inc1.setUpdatedAt(now.minusHours(2));
        inc1.addIncidentNode(createNode(router, IncidentNodeRole.ROOT));
        inc1.addIncidentNode(createNode(gnb1, IncidentNodeRole.AFFECTED));
        inc1.addIncidentNode(createNode(gnb2, IncidentNodeRole.AFFECTED));

        Incident inc2 = new Incident();
        inc2.setIncidentNumber("INC-002");
        inc2.setTitle("5G cell degradation");
        inc2.setStatus(IncidentStatus.ACKNOWLEDGED);
        inc2.setPriority(IncidentPriority.MEDIUM);
        inc2.setRegion("MAZOWIECKIE");
        inc2.setSourceAlarmType("PERFORMANCE");
        inc2.setPossiblyPlanned(false);
        inc2.setRootNode(gnb1);
        inc2.setOpenedAt(now.minusHours(6));
        inc2.setAcknowledgedAt(now.minusHours(5));
        inc2.setCreatedAt(now.minusHours(6));
        inc2.setUpdatedAt(now.minusHours(5));
        inc2.addIncidentNode(createNode(gnb1, IncidentNodeRole.ROOT));
        inc2.addIncidentNode(createNode(gnb2, IncidentNodeRole.AFFECTED));

        Incident inc3 = new Incident();
        inc3.setIncidentNumber("INC-003");
        inc3.setTitle("SBC overload");
        inc3.setStatus(IncidentStatus.RESOLVED);
        inc3.setPriority(IncidentPriority.CRITICAL);
        inc3.setRegion("SLASKIE");
        inc3.setSourceAlarmType("CAPACITY");
        inc3.setPossiblyPlanned(false);
        inc3.setRootNode(sbc);
        inc3.setOpenedAt(now.minusDays(1));
        inc3.setAcknowledgedAt(now.minusHours(20));
        inc3.setResolvedAt(now.minusHours(4));
        inc3.setCreatedAt(now.minusDays(1));
        inc3.setUpdatedAt(now.minusHours(4));
        inc3.addIncidentNode(createNode(sbc, IncidentNodeRole.ROOT));
        inc3.addIncidentNode(createNode(router, IncidentNodeRole.AFFECTED));
        inc3.addIncidentNode(createNode(gnb1, IncidentNodeRole.AFFECTED));

        Incident inc4 = new Incident();
        inc4.setIncidentNumber("INC-004");
        inc4.setTitle("Planned LTE maintenance");
        inc4.setStatus(IncidentStatus.OPEN);
        inc4.setPriority(IncidentPriority.LOW);
        inc4.setRegion("MALOPOLSKIE");
        inc4.setSourceAlarmType("MAINTENANCE");
        inc4.setPossiblyPlanned(true);
        inc4.setRootNode(enb);
        inc4.setOpenedAt(now.minusDays(2));
        inc4.setCreatedAt(now.minusDays(2));
        inc4.setUpdatedAt(now.minusDays(2));
        inc4.addIncidentNode(createNode(enb, IncidentNodeRole.ROOT));

        Incident inc5 = new Incident();
        inc5.setIncidentNumber("INC-005");
        inc5.setTitle("Packet loss issue");
        inc5.setStatus(IncidentStatus.CLOSED);
        inc5.setPriority(IncidentPriority.HIGH);
        inc5.setRegion("MAZOWIECKIE");
        inc5.setSourceAlarmType("NETWORK");
        inc5.setPossiblyPlanned(false);
        inc5.setRootNode(gnb2);
        inc5.setOpenedAt(now.minusDays(5));
        inc5.setAcknowledgedAt(now.minusDays(5).plusHours(1));
        inc5.setResolvedAt(now.minusDays(4));
        inc5.setCreatedAt(now.minusDays(5));
        inc5.setUpdatedAt(now.minusDays(4));
        inc5.addIncidentNode(createNode(gnb2, IncidentNodeRole.ROOT));
        inc5.addIncidentNode(createNode(gnb1, IncidentNodeRole.AFFECTED));

        Incident inc6 = new Incident();
        inc6.setIncidentNumber("INC-006");
        inc6.setTitle("gNodeB reboot loop");
        inc6.setStatus(IncidentStatus.OPEN);
        inc6.setPriority(IncidentPriority.CRITICAL);
        inc6.setRegion("POMORSKIE");
        inc6.setSourceAlarmType("HARDWARE");
        inc6.setPossiblyPlanned(false);
        inc6.setRootNode(gnb1);
        inc6.setOpenedAt(now.minusHours(10));
        inc6.setCreatedAt(now.minusHours(10));
        inc6.setUpdatedAt(now.minusHours(10));
        inc6.addIncidentNode(createNode(gnb1, IncidentNodeRole.ROOT));
        inc6.addIncidentNode(createNode(enb, IncidentNodeRole.AFFECTED));
        inc6.addIncidentNode(createNode(sbc, IncidentNodeRole.AFFECTED));

        incidentRepository.saveAll(List.of(inc1, inc2, inc3, inc4, inc5, inc6));
        incidentRepository.flush();
    }

    private void seedMaintenanceWindowsIfEmpty() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM maintenance_window",
                Long.class
        );

        if (count != null && count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO maintenance_window (title, description, status, start_time, end_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "Planned RAN upgrade - Krakow",
                "Software upgrade for LTE/5G access nodes",
                "PLANNED",
                now.plusDays(1),
                now.plusDays(1).plusHours(4),
                now
        );

        jdbcTemplate.update("""
                INSERT INTO maintenance_window (title, description, status, start_time, end_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "Core SBC patching",
                "Security patch deployment on SBC layer",
                "COMPLETED",
                now.minusDays(3),
                now.minusDays(3).plusHours(2),
                now.minusDays(4)
        );

        Long mw1Id = jdbcTemplate.queryForObject(
                "SELECT id FROM maintenance_window WHERE title = ?",
                Long.class,
                "Planned RAN upgrade - Krakow"
        );

        Long mw2Id = jdbcTemplate.queryForObject(
                "SELECT id FROM maintenance_window WHERE title = ?",
                Long.class,
                "Core SBC patching"
        );

        Long enbId = getNodeIdByName("RAN-ENB-WAW-01");
        Long gnb1Id = getNodeIdByName("RAN-GNB-WAW-01");
        Long sbcId = getNodeIdByName("CORE-SBC-WAW-01");

        jdbcTemplate.update("""
                INSERT INTO maintenance_node (maintenance_window_id, network_node_id, created_at)
                VALUES (?, ?, ?)
                """,
                mw1Id, enbId, now
        );

        jdbcTemplate.update("""
                INSERT INTO maintenance_node (maintenance_window_id, network_node_id, created_at)
                VALUES (?, ?, ?)
                """,
                mw1Id, gnb1Id, now
        );

        jdbcTemplate.update("""
                INSERT INTO maintenance_node (maintenance_window_id, network_node_id, created_at)
                VALUES (?, ?, ?)
                """,
                mw2Id, sbcId, now.minusDays(4)
        );
    }

    private void seedAlarmEventsIfEmpty() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alarm_event",
                Long.class
        );

        if (count != null && count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Long routerId = getNodeIdByName("CORE-RTR-WAW-01");
        Long gnb1Id = getNodeIdByName("RAN-GNB-WAW-01");
        Long gnb2Id = getNodeIdByName("RAN-GNB-WAW-02");
        Long enbId = getNodeIdByName("RAN-ENB-WAW-01");
        Long sbcId = getNodeIdByName("CORE-SBC-WAW-01");

        Long inc1Id = getIncidentIdByNumber("INC-001");
        Long inc2Id = getIncidentIdByNumber("INC-002");
        Long inc3Id = getIncidentIdByNumber("INC-003");
        Long inc4Id = getIncidentIdByNumber("INC-004");
        Long inc6Id = getIncidentIdByNumber("INC-006");

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "OSS",
                "ALARM-001",
                routerId,
                inc1Id,
                "LINK_DOWN",
                "MAJOR",
                "OPEN",
                "Core router uplink down",
                false,
                now.minusHours(2),
                now.minusHours(2).plusMinutes(1),
                now.minusHours(2)
        );

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "OSS",
                "ALARM-002",
                gnb1Id,
                inc2Id,
                "CELL_DEGRADED",
                "MINOR",
                "ACKNOWLEDGED",
                "Degradation detected on 5G cell",
                false,
                now.minusHours(6),
                now.minusHours(6).plusMinutes(2),
                now.minusHours(6)
        );

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "EMS",
                "ALARM-003",
                sbcId,
                inc3Id,
                "CPU_HIGH",
                "CRITICAL",
                "CLEARED",
                "SBC CPU overload",
                false,
                now.minusDays(1),
                now.minusDays(1).plusMinutes(1),
                now.minusDays(1)
        );

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "OSS",
                "ALARM-004",
                enbId,
                inc4Id,
                "MAINTENANCE_MODE",
                "INFO",
                "OPEN",
                "Node in maintenance mode",
                true,
                now.minusDays(2),
                now.minusDays(2).plusMinutes(1),
                now.minusDays(2)
        );

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "EMS",
                "ALARM-005",
                gnb2Id,
                null,
                "PACKET_LOSS",
                "MAJOR",
                "OPEN",
                "Packet loss detected but not yet correlated",
                false,
                now.minusHours(3),
                now.minusHours(3).plusMinutes(1),
                now.minusHours(3)
        );

        jdbcTemplate.update("""
                INSERT INTO alarm_event (
                    source_system, external_id, network_node_id, incident_id,
                    alarm_type, severity, status, description,
                    suppressed_by_maintenance, occurred_at, received_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "OSS",
                "ALARM-006",
                gnb1Id,
                inc6Id,
                "REBOOT_LOOP",
                "CRITICAL",
                "OPEN",
                "Repeated reboot loop on gNodeB",
                false,
                now.minusHours(10),
                now.minusHours(10).plusMinutes(1),
                now.minusHours(10)
        );
    }

    private void seedIncidentTimelineIfEmpty() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_timeline",
                Long.class
        );

        if (count != null && count > 0) {
            return;
        }

        Long inc1Id = getIncidentIdByNumber("INC-001");
        Long inc2Id = getIncidentIdByNumber("INC-002");
        Long inc3Id = getIncidentIdByNumber("INC-003");
        Long inc5Id = getIncidentIdByNumber("INC-005");
        Long inc6Id = getIncidentIdByNumber("INC-006");

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO incident_timeline (incident_id, event_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """,
                inc1Id, "CREATED", "Incident created from router failure alarm", now.minusHours(2)
        );

        jdbcTemplate.update("""
                INSERT INTO incident_timeline (incident_id, event_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """,
                inc2Id, "ACKNOWLEDGED", "NOC engineer acknowledged cell degradation", now.minusHours(5)
        );

        jdbcTemplate.update("""
                INSERT INTO incident_timeline (incident_id, event_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """,
                inc3Id, "RESOLVED", "Traffic rebalanced and SBC CPU normalized", now.minusHours(4)
        );

        jdbcTemplate.update("""
                INSERT INTO incident_timeline (incident_id, event_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """,
                inc5Id, "CLOSED", "Packet loss issue closed after verification", now.minusDays(4)
        );

        jdbcTemplate.update("""
                INSERT INTO incident_timeline (incident_id, event_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """,
                inc6Id, "CREATED", "Incident opened due to repeated reboot loop", now.minusHours(10)
        );
    }

    private NetworkNode getNodeByName(String nodeName) {
        return networkNodeRepository.findByNodeName(nodeName)
                .orElseThrow(() -> new IllegalStateException("Network node not found: " + nodeName));
    }

    private Long getNodeIdByName(String nodeName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM network_node WHERE node_name = ?",
                Long.class,
                nodeName
        );
    }

    private Long getIncidentIdByNumber(String incidentNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM incident WHERE incident_number = ?",
                Long.class,
                incidentNumber
        );
    }

    private IncidentNode createNode(NetworkNode node, IncidentNodeRole role) {
        IncidentNode incidentNode = new IncidentNode();
        incidentNode.setNetworkNode(node);
        incidentNode.setRole(role);
        return incidentNode;
    }
}
