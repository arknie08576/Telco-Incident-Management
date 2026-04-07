package pl.telco.incident.mapper;

import org.mapstruct.Mapper;
import pl.telco.incident.dto.NetworkNodeResponse;
import pl.telco.incident.entity.NetworkNode;

@Mapper(componentModel = "spring")
public interface NetworkNodeMapper {

    NetworkNodeResponse toResponse(NetworkNode networkNode);
}
