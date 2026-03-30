package pl.telco.incident.service;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

final class CrudEventLogger {

    private CrudEventLogger() {
    }

    static void log(Logger log, String eventDataset, String eventAction, Map<String, Object> additionalFields) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("eventDataset", eventDataset);
        fields.put("eventCategory", "entity_crud");
        fields.put("eventAction", eventAction);
        fields.putAll(additionalFields);

        log.info("crud_event {}", StructuredArguments.entries(fields));
    }
}
