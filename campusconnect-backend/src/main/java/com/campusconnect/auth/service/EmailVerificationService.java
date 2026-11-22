package com.campusconnect.auth.service;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.auth.domain.EmailVerificationToken;
import com.campusconnect.auth.repository.EmailVerificationTokenRepository;
import com.campusconnect.auth.repository.UserRepository;
import com.campusconnect.common.exception.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final AuthProperties props;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UserRepository users,
                                    ApplicationEventPublisher events, AuthProperties props) {
        this.tokens = tokens; this.users = users; this.events = events; this.props = props;
    }

    /** Joins the caller's transaction (register) or starts its own (resend). The email goes out AFTER commit. */
    @Transactional
    public void issueAndSend(long userId, String email) {
        Instant now = Instant.now();
        tokens.invalidateAllForUser(userId, now);                       // older links stop working
        String raw = SecureTokens.newToken();
        tokens.save(new EmailVerificationToken(userId, SecureTokens.sha256(raw),
                now.plus(props.emailVerificationHours(), ChronoUnit.HOURS)));
        events.publishEvent(new VerificationEmailRequested(email, raw));
    }

    @Transactional
    public void verify(String raw) {
        Instant now = Instant.now();
        EmailVerificationToken t = tokens.findByTokenHash(SecureTokens.sha256(raw))
                .orElseThrow(EmailVerificationService::invalid);
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(now)) throw invalid();
        if (tokens.markUsed(t.getId(), now) == 0) throw invalid();      // a parallel click got there first
        users.activate(t.getUserId(), now);                             // 0 rows is fine (already active / suspended)
    }

    private static ApiException invalid() {
        // Not "TOKEN_INVALID": React treats that code as "access token bad, go to login".
        return ApiException.badRequest("VERIFICATION_TOKEN_INVALID", "This link is invalid or has expired");
    }
}