package pl.telco.incident.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApplicationLifecycleLogger {

    private final ObservabilityEventLogger observabilityEventLogger;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("applicationState", "READY");

        observabilityEventLogger.logEvent(
                "system",
                "system",
                "startup",
                "application_ready",
                fields
        );
    }
}
