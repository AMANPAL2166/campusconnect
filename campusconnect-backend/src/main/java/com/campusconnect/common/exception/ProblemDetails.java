package com.campusconnect.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.io.IOException;
import java.util.UUID;

/** One place that builds the error body (RFC 7807 + code + traceId). Used by the advice AND the security handlers. */
public final class ProblemDetails {
    private ProblemDetails() {}

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        pd.setProperty("traceId", UUID.randomUUID().toString());
        return pd;
    }

    /** Filters run before controllers, so @RestControllerAdvice can't help. We write JSON by hand. */
    public static void write(HttpServletResponse res, ObjectMapper mapper, ProblemDetail pd) throws IOException {
        res.setStatus(pd.getStatus());
        res.setContentType("application/problem+json");
        mapper.writeValue(res.getOutputStream(), pd);
    }
}