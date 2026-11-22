package com.campusconnect.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** DEV ONLY. Prints the email (including the verification link) instead of sending it. */
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log")
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    LoggingEmailSender() { log.warn("DEV EMAIL MODE: emails are only logged, never sent. Do not use in production."); }

    @Override
    public void send(String to, String subject, String body) {
        log.info("DEV EMAIL to={} subject={}\n{}", to, subject, body);
    }
}