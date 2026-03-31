package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.Region;
import pl.telco.incident.entity.enums.SourceAlarmType;

@Schema(name = "IncidentUpdateRequest", description = "Partial payload used to update editable incident fields.")
public class IncidentUpdateRequest {

    @Schema(description = "Unique incident number visible to operators.", example = "INC-101")
    @Pattern(regexp = ".*\\S.*", message = "incidentNumber must not be blank")
    @Size(max = 50, message = "incidentNumber must not exceed 50 characters")
    private String incidentNumber;

    @Schema(description = "Short incident title.", example = "Router failure in Warsaw")
    @Pattern(regexp = ".*\\S.*", message = "title must not be blank")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Schema(description = "Business priority of the incident.", example = "CRITICAL")
    private IncidentPriority priority;

    @Schema(description = "Affected network region.", example = "SLASKIE")
    private Region region;

    @Schema(description = "Source alarm type reported by monitoring.", example = "POWER")
    private SourceAlarmType sourceAlarmType;

    @Schema(description = "Marks whether the incident may be related to planned maintenance.", example = "true")
    private Boolean possiblyPlanned;

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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public SourceAlarmType getSourceAlarmType() {
        return sourceAlarmType;
    }

    public void setSourceAlarmType(SourceAlarmType sourceAlarmType) {
        this.sourceAlarmType = sourceAlarmType;
    }

    public Boolean getPossiblyPlanned() {
        return possiblyPlanned;
    }

    public void setPossiblyPlanned(Boolean possiblyPlanned) {
        this.possiblyPlanned = possiblyPlanned;
    }
}
