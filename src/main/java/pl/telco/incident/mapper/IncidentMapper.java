package pl.telco.incident.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.telco.incident.dto.IncidentNodeResponse;
import pl.telco.incident.dto.IncidentResponse;
import pl.telco.incident.dto.IncidentSummaryResponse;
import pl.telco.incident.dto.IncidentTimelineResponse;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.IncidentNode;
import pl.telco.incident.entity.IncidentTimeline;

@Mapper(componentModel = "spring")
public interface IncidentMapper {

    IncidentSummaryResponse toSummaryResponse(Incident incident);

    @Mapping(source = "rootNode.id", target = "rootNodeId")
    @Mapping(source = "incidentNodes", target = "nodes")
    IncidentResponse toDetailedResponse(Incident incident);

    @Mapping(source = "networkNode.id", target = "networkNodeId")
    @Mapping(source = "networkNode.nodeName", target = "nodeName")
    @Mapping(source = "networkNode.nodeType", target = "nodeType")
    @Mapping(source = "networkNode.region", target = "region")
    @Mapping(source = "networkNode.vendor", target = "vendor")
    @Mapping(source = "networkNode.active", target = "active")
    IncidentNodeResponse toNodeResponse(IncidentNode incidentNode);

    IncidentTimelineResponse toTimelineResponse(IncidentTimeline timeline);
}
