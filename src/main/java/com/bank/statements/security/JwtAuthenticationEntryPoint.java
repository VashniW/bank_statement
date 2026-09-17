package com.bank.statements.security;

import com.bank.statements.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this, a missing/invalid/expired JWT gets handled by Spring
 * Security's default entry point BEFORE the request ever reaches a
 * controller or @RestControllerAdvice - resulting in a blank 403 with no
 * body, regardless of what GlobalExceptionHandler says. This makes the
 * response consistent with every other error in the API.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(401, "UNAUTHORIZED",
                "Authentication required - missing, invalid, or expired token");

        objectMapper.writeValue(response.getWriter(), body);
    }
}
