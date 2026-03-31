package pl.telco.incident.observability;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.util.Map;

public class TelcoAuditEntityListener {

    @PostPersist
    public void postPersist(Object entity) {
        logEntityChange(entity, "insert");
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        logEntityChange(entity, "update");
    }

    @PostRemove
    public void postRemove(Object entity) {
        logEntityChange(entity, "delete");
    }

    private void logEntityChange(Object entity, String operation) {
        AuditFieldExtractor extractor = SpringContextHolder.getBean(AuditFieldExtractor.class);
        ObservabilityEventLogger logger = SpringContextHolder.getBean(ObservabilityEventLogger.class);

        Map<String, Object> fields = extractor.extract(entity, operation);
        String dataset = (String) fields.remove("eventDataset");

        logger.logEvent(
                dataset,
                "persistence",
                operation,
                "entity_change",
                fields
        );
    }
}
