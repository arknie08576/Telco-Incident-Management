package pl.telco.incident.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.Set;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) ->
                priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> hasPriorities(Set<IncidentPriority> priorities) {
        return (root, query, cb) ->
                (priorities == null || priorities.isEmpty()) ? null : root.get("priority").in(priorities);
    }

    public static Specification<Incident> hasRegion(String region) {
        return (root, query, cb) ->
                (region == null || region.isBlank())
                        ? null
                        : cb.equal(cb.lower(root.get("region")), region.trim().toLowerCase());
    }

    public static Specification<Incident> hasPossiblyPlanned(Boolean possiblyPlanned) {
        return (root, query, cb) ->
                possiblyPlanned == null ? null : cb.equal(root.get("possiblyPlanned"), possiblyPlanned);
    }

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasStatuses(Set<IncidentStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<Incident> incidentNumberContains(String incidentNumber) {
        return (root, query, cb) ->
                (incidentNumber == null || incidentNumber.isBlank())
                        ? null
                        : cb.like(
                        cb.lower(root.get("incidentNumber")),
                        "%" + incidentNumber.trim().toLowerCase() + "%"
                );
    }

    public static Specification<Incident> titleContains(String title) {
        return (root, query, cb) ->
                (title == null || title.isBlank())
                        ? null
                        : cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.trim().toLowerCase() + "%"
                );
    }

    public static Specification<Incident> hasSourceAlarmType(String sourceAlarmType) {
        return (root, query, cb) ->
                (sourceAlarmType == null || sourceAlarmType.isBlank())
                        ? null
                        : cb.equal(
                        cb.lower(root.get("sourceAlarmType")),
                        sourceAlarmType.trim().toLowerCase()
                );
    }

    public static Specification<Incident> openedAtFrom(LocalDateTime openedFrom) {
        return (root, query, cb) ->
                openedFrom == null ? null : cb.greaterThanOrEqualTo(root.get("openedAt"), openedFrom);
    }

    public static Specification<Incident> openedAtTo(LocalDateTime openedTo) {
        return (root, query, cb) ->
                openedTo == null ? null : cb.lessThanOrEqualTo(root.get("openedAt"), openedTo);
    }

    public static Specification<Incident> acknowledgedAtFrom(LocalDateTime acknowledgedFrom) {
        return (root, query, cb) ->
                acknowledgedFrom == null ? null : cb.greaterThanOrEqualTo(root.get("acknowledgedAt"), acknowledgedFrom);
    }

    public static Specification<Incident> acknowledgedAtTo(LocalDateTime acknowledgedTo) {
        return (root, query, cb) ->
                acknowledgedTo == null ? null : cb.lessThanOrEqualTo(root.get("acknowledgedAt"), acknowledgedTo);
    }

    public static Specification<Incident> resolvedAtFrom(LocalDateTime resolvedFrom) {
        return (root, query, cb) ->
                resolvedFrom == null ? null : cb.greaterThanOrEqualTo(root.get("resolvedAt"), resolvedFrom);
    }

    public static Specification<Incident> resolvedAtTo(LocalDateTime resolvedTo) {
        return (root, query, cb) ->
                resolvedTo == null ? null : cb.lessThanOrEqualTo(root.get("resolvedAt"), resolvedTo);
    }

    public static Specification<Incident> closedAtFrom(LocalDateTime closedFrom) {
        return (root, query, cb) ->
                closedFrom == null ? null : cb.greaterThanOrEqualTo(root.get("closedAt"), closedFrom);
    }

    public static Specification<Incident> closedAtTo(LocalDateTime closedTo) {
        return (root, query, cb) ->
                closedTo == null ? null : cb.lessThanOrEqualTo(root.get("closedAt"), closedTo);
    }
}
