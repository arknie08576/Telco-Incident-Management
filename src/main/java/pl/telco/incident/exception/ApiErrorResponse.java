package pl.telco.incident.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

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

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiErrorResponse(LocalDateTime timestamp, int status, String error, String message,
                            String path, Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
