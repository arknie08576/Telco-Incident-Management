package pl.telco.incident.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.entity.AlarmEvent;

@Mapper(componentModel = "spring")
public interface AlarmEventMapper {

    @Mapping(source = "networkNode.id", target = "networkNodeId")
    @Mapping(source = "incident.id", target = "incidentId")
    AlarmEventResponse toResponse(AlarmEvent alarmEvent);
}
