package com.example.demo.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.exception.ApiError.FieldValidationError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Centralized translation of exceptions to the standard {@link ApiError} body
 * (per {@code ../golden/api-standards.md}).
 *
 * <p>Keeps error handling out of controllers and guarantees responses never leak
 * stack traces, SQL, or server internals (FR-4, security standards).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation failures on query parameters (e.g. negative page, size &lt; 1). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<FieldValidationError> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();

        log.warn("Validation failure on {}: {} field error(s)", path(request), fieldErrors.size());

        ApiError error = ApiError.of(HttpStatus.BAD_REQUEST,
                "Request validation failed", path(request), fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    /** Non-numeric or otherwise unconvertible query parameters (e.g. {@code page=abc}). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        FieldValidationError fieldError = new FieldValidationError(
                ex.getName(), "must be a valid " + simpleTypeName(ex) + " value");

        log.warn("Type mismatch on {} for parameter '{}'", path(request), ex.getName());

        ApiError error = ApiError.of(HttpStatus.BAD_REQUEST,
                "Request validation failed", path(request), List.of(fieldError));
        return ResponseEntity.badRequest().body(error);
    }

    /** Fallback: never expose internals; log the detail server-side only. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error handling {}", path(request), ex);

        ApiError error = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", path(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private FieldValidationError toFieldError(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDot = propertyPath.lastIndexOf('.');
        String field = lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
        return new FieldValidationError(field, violation.getMessage());
    }

    private String simpleTypeName(MethodArgumentTypeMismatchException ex) {
        return ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "value";
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
