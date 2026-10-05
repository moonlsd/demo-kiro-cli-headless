package com.example.demo.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Standard, client-safe error body shared by every endpoint (RFC 7807-style
 * problem detail), per {@code ../golden/api-standards.md}.
 *
 * <p>Produced centrally by {@link GlobalExceptionHandler} and the security entry
 * point. It never carries stack traces, SQL, or internal details.
 *
 * @param timestamp       when the error was produced (ISO-8601 UTC)
 * @param status          the HTTP status code
 * @param error           the HTTP reason phrase
 * @param message         a safe, human-readable summary
 * @param path            the request path that produced the error
 * @param fieldErrors     per-field validation messages (empty when not applicable)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors) {

    /** A single field-level validation failure. */
    public record FieldValidationError(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String message, String path,
            List<FieldValidationError> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
