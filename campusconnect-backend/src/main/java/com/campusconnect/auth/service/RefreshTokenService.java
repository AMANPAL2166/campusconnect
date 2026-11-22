package com.campusconnect.auth.service;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.auth.domain.RefreshToken;
import com.campusconnect.auth.repository.RefreshTokenRepository;
import com.campusconnect.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenService {

    public record Rotated(long userId, String newToken) {}

    private final RefreshTokenRepository tokens;
    private final AuthProperties props;

    public RefreshTokenService(RefreshTokenRepository tokens, AuthProperties props) {
        this.tokens = tokens;
        this.props = props;
    }

    @Transactional
    public String issue(long userId) {
        String raw = SecureTokens.newToken();
        tokens.save(new RefreshToken(userId, SecureTokens.sha256(raw),
                Instant.now().plus(props.refreshTokenDays(), ChronoUnit.DAYS)));
        return raw;
    }

    /**
     * Rotation: the presented token dies, a new one is born, in one transaction.
     *
     * noRollbackFor is REQUIRED. On reuse we write (revoke all) and THEN throw 401. With the default
     * rollback-on-RuntimeException, the "revoke all" would be undone and the theft response would silently do nothing.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotated rotate(String raw) {
        Instant now = Instant.now();
        RefreshToken t = tokens.findByTokenHash(SecureTokens.sha256(raw)).orElseThrow(RefreshTokenService::invalid);

        if (t.getRevokedAt() != null) {
            // A used token came back: harmless double-submit (two tabs) or a stolen copy.
            // Inside the grace window we only refuse. Outside it we assume theft and end ALL sessions.
            if (t.getRevokedAt().isBefore(now.minusSeconds(props.refreshReuseGraceSeconds()))) {
                tokens.revokeAllForUser(t.getUserId(), now);
            }
            throw invalid();
        }
        if (t.getExpiresAt().isBefore(now)) throw invalid();
        if (tokens.revoke(t.getId(), now) == 0) throw invalid();   // lost the race to a parallel request

        return new Rotated(t.getUserId(), issue(t.getUserId()));   // self-call is fine: we're already in rotate's tx
    }

    @Transactional
    public void revokeByRaw(String raw) {
        tokens.findByTokenHash(SecureTokens.sha256(raw)).ifPresent(t -> tokens.revoke(t.getId(), Instant.now()));
    }

    @Transactional
    public void revokeAll(long userId) {
        tokens.revokeAllForUser(userId, Instant.now());
    }

    private static ApiException invalid() {
        return ApiException.unauthorized("REFRESH_TOKEN_INVALID", "Please log in again");
    }
}