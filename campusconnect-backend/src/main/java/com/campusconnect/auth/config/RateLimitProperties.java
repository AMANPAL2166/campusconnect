package com.campusconnect.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int registerPerIp, int loginPerIp, int loginFailuresPerEmail,
                                  int resendPerIp, int resendPerEmail, int refreshPerIp) {}