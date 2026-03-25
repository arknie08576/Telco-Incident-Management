package pl.telco.incident.dto;

import lombok.Data;
import pl.telco.incident.entity.enums.IncidentNodeRole;

@Data
public class IncidentNodeRequest {

    private Long networkNodeId;

    private IncidentNodeRole role;
}