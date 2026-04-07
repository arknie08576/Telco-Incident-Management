package pl.telco.incident.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.telco.incident.dto.MaintenanceWindowResponse;
import pl.telco.incident.entity.MaintenanceNode;
import pl.telco.incident.entity.MaintenanceWindow;

@Mapper(componentModel = "spring")
public interface MaintenanceWindowMapper {

    @Mapping(source = "maintenanceNodes", target = "nodeIds")
    MaintenanceWindowResponse toResponse(MaintenanceWindow maintenanceWindow);

    default Long maintenanceNodeToNodeId(MaintenanceNode node) {
        return node.getNetworkNode().getId();
    }
}
