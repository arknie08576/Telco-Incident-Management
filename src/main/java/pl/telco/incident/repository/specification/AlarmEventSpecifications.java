package pl.telco.incident.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.entity.AlarmEvent;
import pl.telco.incident.entity.enums.AlarmSeverity;
import pl.telco.incident.entity.enums.AlarmStatus;

import java.time.LocalDateTime;
import java.util.Set;

public final class AlarmEventSpecifications {

    private AlarmEventSpecifications() {
    }

    public static Specification<AlarmEvent> hasSeverities(Set<AlarmSeverity> severities) {
        return (root, query, cb) ->
                (severities == null || severities.isEmpty()) ? null : root.get("severity").in(severities);
    }

    public static Specification<AlarmEvent> hasStatuses(Set<AlarmStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<AlarmEvent> sourceSystemContains(String sourceSystem) {
        return containsIgnoreCase("sourceSystem", sourceSystem);
    }

    public static Specification<AlarmEvent> externalIdContains(String externalId) {
        return containsIgnoreCase("externalId", externalId);
    }

    public static Specification<AlarmEvent> alarmTypeContains(String alarmType) {
        return containsIgnoreCase("alarmType", alarmType);
    }

    public static Specification<AlarmEvent> hasNetworkNodeId(Long networkNodeId) {
        return (root, query, cb) ->
                networkNodeId == null ? null : cb.equal(root.get("networkNode").get("id"), networkNodeId);
    }

    public static Specification<AlarmEvent> hasIncidentId(Long incidentId) {
        return (root, query, cb) ->
                incidentId == null ? null : cb.equal(root.get("incident").get("id"), incidentId);
    }

    public static Specification<AlarmEvent> hasSuppressedByMaintenance(Boolean suppressedByMaintenance) {
        return (root, query, cb) ->
                suppressedByMaintenance == null ? null : cb.equal(root.get("suppressedByMaintenance"), suppressedByMaintenance);
    }

    public static Specification<AlarmEvent> occurredAtFrom(LocalDateTime occurredFrom) {
        return (root, query, cb) ->
                occurredFrom == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom);
    }

    public static Specification<AlarmEvent> occurredAtTo(LocalDateTime occurredTo) {
        return (root, query, cb) ->
                occurredTo == null ? null : cb.lessThanOrEqualTo(root.get("occurredAt"), occurredTo);
    }

    public static Specification<AlarmEvent> receivedAtFrom(LocalDateTime receivedFrom) {
        return (root, query, cb) ->
                receivedFrom == null ? null : cb.greaterThanOrEqualTo(root.get("receivedAt"), receivedFrom);
    }

    public static Specification<AlarmEvent> receivedAtTo(LocalDateTime receivedTo) {
        return (root, query, cb) ->
                receivedTo == null ? null : cb.lessThanOrEqualTo(root.get("receivedAt"), receivedTo);
    }

    private static Specification<AlarmEvent> containsIgnoreCase(String field, String value) {
        return (root, query, cb) ->
                (value == null || value.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
    }
}
