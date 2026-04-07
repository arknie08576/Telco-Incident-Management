package pl.telco.incident.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "Standard error payload returned by the API.")
public class ApiErrorResponse {

    @Schema(description = "Timestamp when the error response was generated.", example = "2026-03-29T06:43:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "HTTP reason phrase.", example = "Bad Request")
    private String error;

    @Schema(description = "Application error message.", example = "Validation failed")
    private String message;

    @Schema(description = "Request path that triggered the error.", example = "/api/incidents")
    private String path;

    @Schema(description = "Field-level validation details when present.")
    private Map<String, String> fieldErrors;
}
