package pl.telco.incident.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        long startedAt = System.nanoTime();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            log.info(
                    "http_request_completed {} {} {} {} {} {} {} {} {}",
                    StructuredArguments.keyValue("eventDataset", "http"),
                    StructuredArguments.keyValue("eventCategory", "transport"),
                    StructuredArguments.keyValue("eventAction", "request_completed"),
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("query", request.getQueryString()),
                    StructuredArguments.keyValue("status", response.getStatus()),
                    StructuredArguments.keyValue("durationMs", durationMs),
                    StructuredArguments.keyValue("clientIp", request.getRemoteAddr())
            );

            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(String incomingRequestId) {
        if (incomingRequestId == null || incomingRequestId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return incomingRequestId.trim();
    }
}
