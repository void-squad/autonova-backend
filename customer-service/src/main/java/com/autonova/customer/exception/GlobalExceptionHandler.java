package com.autonova.customer.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.debug("Validation error on {}: {}", request.getRequestURI(), fieldErrors);
        return buildError(status, "Validation failed", fieldErrors, request.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus effectiveStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug("Request {} failed with status {}: {}", request.getRequestURI(), effectiveStatus, ex.getReason());
        return buildError(effectiveStatus, ex.getReason(), null, request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return buildError(status, sanitizeMessage(message), null, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return buildError(status, "Unexpected error", null, request.getRequestURI());
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String message, List<ApiError.FieldError> fieldErrors, String path) {
        String safeMessage = StringUtils.hasText(message) ? message : status.getReasonPhrase();
        ApiError body = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                safeMessage,
                fieldErrors == null ? List.of() : fieldErrors,
                path,
                OffsetDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    private ApiError.FieldError toFieldError(FieldError fieldError) {
        return new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String sanitizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Request violates a data integrity constraint";
        }
        String lowerCased = message.toLowerCase();
        if (lowerCased.contains("duplicate")) {
            return "Duplicate value violates a uniqueness constraint";
        }
        if (lowerCased.contains("foreign key")) {
            return "Related record is missing for this operation";
        }
        return "Request violates a data integrity constraint";
    }
}
