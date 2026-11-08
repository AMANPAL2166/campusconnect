package com.campusconnect.auth.security;

import com.campusconnect.common.exception.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    RestAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        ProblemDetails.write(res, mapper, ProblemDetails.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to do this"));
    }
}
