package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IncidentActionRequest", description = "Optional note attached to lifecycle actions.")
public class IncidentActionRequest {

    @Schema(description = "Optional operator note appended to the lifecycle timeline entry.", example = "Traffic rerouted")
    private String note;

    public IncidentActionRequest() {
    }

    public IncidentActionRequest(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
