package pl.telco.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Data
public class NetworkNodeCreateRequest {

    @NotBlank(message = "nodeName is required")
    @Size(max = 100, message = "nodeName must not exceed 100 characters")
    private String nodeName;

    @NotNull(message = "nodeType is required")
    private NodeType nodeType;

    @NotNull(message = "region is required")
    private Region region;

    @Size(max = 100, message = "vendor must not exceed 100 characters")
    private String vendor;

    private Boolean active;
}
