package pl.telco.incident.dto;

public class IncidentActionRequest {

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