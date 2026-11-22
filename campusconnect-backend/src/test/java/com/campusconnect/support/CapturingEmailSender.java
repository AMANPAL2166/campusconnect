package com.campusconnect.support;

import com.campusconnect.common.email.EmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test double: remembers the latest email per recipient so tests can read the verification link. */
public class CapturingEmailSender implements EmailSender {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    private final Map<String, String> lastBody = new ConcurrentHashMap<>();

    @Override
    public void send(String to, String subject, String body) { lastBody.put(to, body); }

    public Optional<String> tokenFor(String email) {
        String body = lastBody.get(email);
        if (body == null) return Optional.empty();
        Matcher m = TOKEN.matcher(body);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean @Primary
        CapturingEmailSender capturingEmailSender() { return new CapturingEmailSender(); }
    }
}