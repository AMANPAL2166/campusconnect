package com.campusconnect.auth.service;

import com.campusconnect.auth.config.RateLimitProperties;
import com.campusconnect.common.ratelimit.FixedWindowLimiter;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class AuthRateLimits {
    private static final long MAX_KEYS = 50_000;   // memory cap against key-flooding

    private final FixedWindowLimiter register, login, loginFailures, resend, resendEmail, refresh;

    AuthRateLimits(RateLimitProperties p) {
        register      = new FixedWindowLimiter(p.registerPerIp(),         Duration.ofHours(1),    MAX_KEYS);
        login         = new FixedWindowLimiter(p.loginPerIp(),            Duration.ofMinutes(1),  MAX_KEYS);
        loginFailures = new FixedWindowLimiter(p.loginFailuresPerEmail(), Duration.ofMinutes(15), MAX_KEYS);
        resend        = new FixedWindowLimiter(p.resendPerIp(),           Duration.ofHours(1),    MAX_KEYS);
        resendEmail   = new FixedWindowLimiter(p.resendPerEmail(),        Duration.ofHours(1),    MAX_KEYS);
        refresh       = new FixedWindowLimiter(p.refreshPerIp(),          Duration.ofMinutes(1),  MAX_KEYS);
    }

    FixedWindowLimiter register()      { return register; }
    FixedWindowLimiter login()         { return login; }
    FixedWindowLimiter loginFailures() { return loginFailures; }
    FixedWindowLimiter resend()        { return resend; }
    FixedWindowLimiter resendEmail()   { return resendEmail; }
    FixedWindowLimiter refresh()       { return refresh; }
}