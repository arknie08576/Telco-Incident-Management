package pl.telco.incident.observability;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class ObservabilityEventLogger {

    public void logEvent(
            String dataset,
            String category,
            String action,
            String message,
            Map<String, Object> additionalFields
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("eventDataset", dataset);
        fields.put("eventCategory", category);
        fields.put("eventAction", action);

        if (additionalFields != null && !additionalFields.isEmpty()) {
            fields.putAll(additionalFields);
        }

        log.info("{} {}", message, StructuredArguments.entries(fields));
    }
}
