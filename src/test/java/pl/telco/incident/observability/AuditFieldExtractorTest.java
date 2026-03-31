package pl.telco.incident.observability;

import org.junit.jupiter.api.Test;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.IncidentNodeRole;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;
import pl.telco.incident.entity.enums.IncidentTimelineEventType;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldExtractorTest {

    private final AuditFieldExtractor extractor = new AuditFieldExtractor();

    @Test
    void shouldExtractIncidentFields() {
        NetworkNode rootNode = NetworkNode.builder()
                .id(10L)
                .nodeName("CORE-RTR-WAW-01")
                .nodeType(NodeType.ROUTER)
                .region(Region.MAZOWIECKIE)
                .active(true)
                .build();

        Incident incident = Incident.builder()
                .id(42L)
                .incidentNumber("INC-42")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .region(Region.MAZOWIECKIE)
                .sourceAlarmType(SourceAlarmType.HARDWARE)
                .possiblyPlanned(false)
                .rootNode(rootNode)
                .build();

        Map<String, Object> fields = extractor.extract(incident, "insert");

        assertThat(fields.get("eventDataset")).isEqualTo("incident");
        assertThat(fields.get("entityType")).isEqualTo("Incident");
        assertThat(fields.get("entityId")).isEqualTo(42L);
        assertThat(fields.get("incidentNumber")).isEqualTo("INC-42");
        assertThat(fields.get("rootNodeId")).isEqualTo(10L);
    }

    @Test
    void shouldExtractNetworkNodeFields() {
        NetworkNode networkNode = NetworkNode.builder()
                .id(7L)
                .nodeName("RAN-GNB-KRK-01")
                .nodeType(NodeType.G_NODE_B)
                .region(Region.MALOPOLSKIE)
                .vendor("Ericsson")
                .active(true)
                .build();

        Map<String, Object> fields = extractor.extract(networkNode, "update");

        assertThat(fields.get("eventDataset")).isEqualTo("network_node");
        assertThat(fields.get("entityType")).isEqualTo("NetworkNode");
        assertThat(fields.get("nodeName")).isEqualTo("RAN-GNB-KRK-01");
        assertThat(fields.get("nodeType")).isEqualTo(NodeType.G_NODE_B);
    }

    @Test
    void shouldExtractIncidentChildFields() {
        Incident incident = Incident.builder().id(55L).build();
        NetworkNode networkNode = NetworkNode.builder().id(5L).build();

        IncidentNode incidentNode = new IncidentNode();
        incidentNode.setId(501L);
        incidentNode.setIncident(incident);
        incidentNode.setNetworkNode(networkNode);
        incidentNode.setRole(IncidentNodeRole.AFFECTED);

        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setIncident(incident);
        timeline.setEventType(IncidentTimelineEventType.RESOLVED);

        Map<String, Object> nodeFields = extractor.extract(incidentNode, "insert");
        Map<String, Object> timelineFields = extractor.extract(timeline, "insert");

        assertThat(nodeFields.get("entityType")).isEqualTo("IncidentNode");
        assertThat(nodeFields.get("incidentId")).isEqualTo(55L);
        assertThat(nodeFields.get("networkNodeId")).isEqualTo(5L);
        assertThat(timelineFields.get("entityType")).isEqualTo("IncidentTimeline");
        assertThat(timelineFields.get("timelineEventType")).isEqualTo(IncidentTimelineEventType.RESOLVED);
    }
}
