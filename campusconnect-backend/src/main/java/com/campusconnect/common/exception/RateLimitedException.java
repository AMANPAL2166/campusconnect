package com.campusconnect.common.exception;

import org.springframework.http.HttpStatus;

public class RateLimitedException extends ApiException {
    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many requests, try again later");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}