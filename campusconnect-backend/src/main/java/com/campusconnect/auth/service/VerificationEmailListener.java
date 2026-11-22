package com.campusconnect.auth.service;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.common.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class VerificationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailListener.class);
    private final EmailSender sender;
    private final AuthProperties props;

    VerificationEmailListener(EmailSender sender, AuthProperties props) { this.sender = sender; this.props = props; }

    // AFTER_COMMIT: never email a user whose registration rolled back.
    // @Async: SMTP is slow, so the HTTP response must not wait for it.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(VerificationEmailRequested e) {
        String link = props.frontendBaseUrl() + "/verify?token=" + e.rawToken();
        try {
            sender.send(e.email(), "Verify your CampusConnect email",
                    "Click to verify (valid for " + props.emailVerificationHours() + " hours):\n" + link);
        } catch (RuntimeException ex) {
            // The user can use "resend". We never log the token or the address.
            log.warn("Verification email could not be sent: {}", ex.getClass().getSimpleName());
        }
    }
}