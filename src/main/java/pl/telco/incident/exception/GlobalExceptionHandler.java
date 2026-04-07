package pl.telco.incident.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            BindException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();

            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        log.warn(
                "validation_failed {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "validation_failed"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.BAD_REQUEST.value()),
                StructuredArguments.keyValue("fieldErrors", fieldErrors)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            fieldErrors.put(fieldName, violation.getMessage());
        });

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        log.warn(
                "constraint_violation {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "constraint_violation"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.BAD_REQUEST.value()),
                StructuredArguments.keyValue("fieldErrors", fieldErrors)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Malformed JSON request or invalid enum value",
                request.getRequestURI(),
                null
        );

        log.warn(
                "http_message_not_readable {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "http_message_not_readable"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.BAD_REQUEST.value())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null
        );

        log.warn(
                "method_argument_type_mismatch {} {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "method_argument_type_mismatch"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.BAD_REQUEST.value()),
                StructuredArguments.keyValue("parameter", ex.getName()),
                StructuredArguments.keyValue("value", ex.getValue())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn(
                "bad_request {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "bad_request"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.BAD_REQUEST.value()),
                StructuredArguments.keyValue("message", ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn(
                "resource_not_found {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "resource_not_found"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.NOT_FOUND.value()),
                StructuredArguments.keyValue("message", ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn(
                "conflict {} {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "conflict"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.CONFLICT.value()),
                StructuredArguments.keyValue("message", ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Database constraint violation",
                request.getRequestURI(),
                null
        );

        log.warn(
                "data_integrity_violation {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "data_integrity_violation"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.CONFLICT.value())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Resource was modified concurrently. Please retry.",
                request.getRequestURI(),
                null
        );

        log.warn(
                "optimistic_lock_conflict {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "optimistic_lock_conflict"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.CONFLICT.value())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected server error",
                request.getRequestURI(),
                null
        );

        log.error(
                "unexpected_server_error {} {} {} {} {} {}",
                StructuredArguments.keyValue("eventDataset", "system"),
                StructuredArguments.keyValue("eventCategory", "application_error"),
                StructuredArguments.keyValue("eventAction", "unexpected_server_error"),
                StructuredArguments.keyValue("method", request.getMethod()),
                StructuredArguments.keyValue("path", request.getRequestURI()),
                StructuredArguments.keyValue("status", HttpStatus.INTERNAL_SERVER_ERROR.value()),
                ex
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
