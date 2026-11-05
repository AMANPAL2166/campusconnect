package com.campusconnect.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(List<String> allowedEmailDomains, boolean requireEmailVerification) {
    public AuthProperties {
        allowedEmailDomains = allowedEmailDomains == null ? List.of()
                : allowedEmailDomains.stream().map(d -> d.toLowerCase(Locale.ROOT)).toList();
    }
}