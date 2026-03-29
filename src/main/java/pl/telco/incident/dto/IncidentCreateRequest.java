package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.IncidentPriority;

import java.util.List;

@Schema(name = "IncidentCreateRequest", description = "Payload used to create a new incident.")
public class IncidentCreateRequest {

    @Schema(description = "Unique incident number visible to operators.", example = "INC-100")
    @NotBlank(message = "incidentNumber is required")
    @Size(max = 50, message = "incidentNumber must not exceed 50 characters")
    private String incidentNumber;

    @Schema(description = "Short incident title.", example = "Router failure in Warsaw")
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Schema(description = "Business priority of the incident.", example = "HIGH")
    @NotNull(message = "priority is required")
    private IncidentPriority priority;

    @Schema(description = "Affected network region.", example = "MAZOWIECKIE")
    @NotBlank(message = "region is required")
    @Size(max = 100, message = "region must not exceed 100 characters")
    private String region;

    @Schema(description = "Source alarm type reported by monitoring.", example = "HARDWARE")
    @Size(max = 50, message = "sourceAlarmType must not exceed 50 characters")
    private String sourceAlarmType;

    @Schema(description = "Marks whether the incident may be related to planned maintenance.", example = "false")
    private Boolean possiblyPlanned;

    @Schema(description = "Identifier of the root network node.", example = "1")
    @NotNull(message = "rootNodeId is required")
    @Positive(message = "rootNodeId must be greater than 0")
    private Long rootNodeId;

    @ArraySchema(
            schema = @Schema(implementation = IncidentNodeRequest.class),
            minItems = 1,
            arraySchema = @Schema(description = "Incident nodes including exactly one ROOT node.")
    )
    @NotEmpty(message = "nodes must not be empty")
    @Valid
    private List<IncidentNodeRequest> nodes;

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public void setIncidentNumber(String incidentNumber) {
        this.incidentNumber = incidentNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSourceAlarmType() {
        return sourceAlarmType;
    }

    public void setSourceAlarmType(String sourceAlarmType) {
        this.sourceAlarmType = sourceAlarmType;
    }

    public Boolean getPossiblyPlanned() {
        return possiblyPlanned;
    }

    public void setPossiblyPlanned(Boolean possiblyPlanned) {
        this.possiblyPlanned = possiblyPlanned;
    }

    public Long getRootNodeId() {
        return rootNodeId;
    }

    public void setRootNodeId(Long rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    public List<IncidentNodeRequest> getNodes() {
        return nodes;
    }

    public void setNodes(List<IncidentNodeRequest> nodes) {
        this.nodes = nodes;
    }
}
