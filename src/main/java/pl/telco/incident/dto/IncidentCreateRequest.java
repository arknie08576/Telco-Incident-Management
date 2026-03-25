package pl.telco.incident.dto;

import lombok.Data;
import java.util.List;

@Data
public class IncidentCreateRequest {

    private String description;

    private List<IncidentNodeRequest> nodes;
}