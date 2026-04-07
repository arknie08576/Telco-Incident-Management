package pl.telco.incident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

@Data
public class NetworkNodeUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "nodeName must not be blank")
    @Size(max = 100, message = "nodeName must not exceed 100 characters")
    private String nodeName;

    private NodeType nodeType;

    private Region region;

    @Pattern(regexp = ".*\\S.*", message = "vendor must not be blank")
    @Size(max = 100, message = "vendor must not exceed 100 characters")
    private String vendor;

    private Boolean active;
}
