package pl.telco.incident.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.entity.MaintenanceWindow;
import pl.telco.incident.entity.enums.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.Set;

public final class MaintenanceWindowSpecifications {

    private MaintenanceWindowSpecifications() {
    }

    public static Specification<MaintenanceWindow> hasStatuses(Set<MaintenanceStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<MaintenanceWindow> titleContains(String title) {
        return (root, query, cb) ->
                (title == null || title.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
    }

    public static Specification<MaintenanceWindow> hasNodeId(Long nodeId) {
        return (root, query, cb) -> {
            if (nodeId == null) {
                return null;
            }

            query.distinct(true);
            return cb.equal(root.join("maintenanceNodes").join("networkNode").get("id"), nodeId);
        };
    }

    public static Specification<MaintenanceWindow> startTimeFrom(LocalDateTime startFrom) {
        return (root, query, cb) ->
                startFrom == null ? null : cb.greaterThanOrEqualTo(root.get("startTime"), startFrom);
    }

    public static Specification<MaintenanceWindow> startTimeTo(LocalDateTime startTo) {
        return (root, query, cb) ->
                startTo == null ? null : cb.lessThanOrEqualTo(root.get("startTime"), startTo);
    }

    public static Specification<MaintenanceWindow> endTimeFrom(LocalDateTime endFrom) {
        return (root, query, cb) ->
                endFrom == null ? null : cb.greaterThanOrEqualTo(root.get("endTime"), endFrom);
    }

    public static Specification<MaintenanceWindow> endTimeTo(LocalDateTime endTo) {
        return (root, query, cb) ->
                endTo == null ? null : cb.lessThanOrEqualTo(root.get("endTime"), endTo);
    }
}
