package com.campusconnect.auth.security;

import com.campusconnect.common.exception.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;
    RestAuthenticationEntryPoint(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        String code = (String) req.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTR);
        ProblemDetails.write(res, mapper, ProblemDetails.of(
                HttpStatus.UNAUTHORIZED, code != null ? code : "TOKEN_INVALID", "Authentication required"));
    }
}