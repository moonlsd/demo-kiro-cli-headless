package com.example.demo.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.example.demo.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the application's standard problem-detail body for unauthenticated
 * requests (KIRODEMO-003, FR-3).
 *
 * <p>Returns {@code 401} with the same {@link ApiError} shape produced by the
 * central {@code @RestControllerAdvice}, and never reveals whether any particular
 * account or order exists.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource",
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
