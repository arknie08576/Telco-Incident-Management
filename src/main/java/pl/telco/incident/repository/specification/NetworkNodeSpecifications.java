package pl.telco.incident.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import pl.telco.incident.entity.NetworkNode;
import pl.telco.incident.entity.enums.NodeType;
import pl.telco.incident.entity.enums.Region;

public final class NetworkNodeSpecifications {

    private NetworkNodeSpecifications() {
    }

    public static Specification<NetworkNode> nameContains(String query) {
        return (root, criteriaQuery, criteriaBuilder) ->
                (query == null || query.isBlank())
                        ? null
                        : criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nodeName")),
                        "%" + query.trim().toLowerCase() + "%"
                );
    }

    public static Specification<NetworkNode> hasRegion(Region region) {
        return (root, criteriaQuery, criteriaBuilder) ->
                region == null ? null : criteriaBuilder.equal(root.get("region"), region);
    }

    public static Specification<NetworkNode> hasNodeType(NodeType nodeType) {
        return (root, criteriaQuery, criteriaBuilder) ->
                nodeType == null ? null : criteriaBuilder.equal(root.get("nodeType"), nodeType);
    }

    public static Specification<NetworkNode> isActive(Boolean active) {
        return (root, criteriaQuery, criteriaBuilder) ->
                active == null ? null : criteriaBuilder.equal(root.get("active"), active);
    }
}
