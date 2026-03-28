package pl.telco.incident.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.entity.Incident;
import pl.telco.incident.entity.enums.IncidentPriority;
import pl.telco.incident.entity.enums.IncidentStatus;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) ->
                priority == null ? null : cb.equal(root.get("priority"), priority);
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
}