package com.campusconnect.auth.service;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.auth.domain.User;
import com.campusconnect.auth.domain.UserStatus;
import com.campusconnect.auth.repository.UserRepository;
import com.campusconnect.auth.web.dto.*;
import com.campusconnect.common.exception.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Deliberately NOT @Transactional. BCrypt takes ~100 ms, and we must not hold a DB connection
 * while hashing (Phase D, D3). The only transaction is inside RegistrationWriter.
 */
@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RegistrationWriter writer;
    private final JwtService jwt;
    private final AuthProperties props;
    private final String dummyHash;   // used so "unknown email" costs the same time as "wrong password"

    public AuthService(UserRepository users, PasswordEncoder encoder, RegistrationWriter writer,
                       JwtService jwt, AuthProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.writer = writer;
        this.jwt = jwt;
        this.props = props;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public RegisterResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        requireAllowedDomain(email);
        if (utf8Length(req.password()) > BCRYPT_MAX_BYTES) {
            // BCrypt silently ignores bytes after 72, so we reject instead of truncating.
            throw ApiException.badRequest("PASSWORD_TOO_LONG", "Password must be at most 72 bytes");
        }
        if (users.existsByEmail(email)) {                       // cheap, friendly fast path
            throw emailTaken();
        }
        String hash = encoder.encode(req.password());           // slow, outside any transaction
        try {
            writer.create(email, hash, req.displayName().trim());
        } catch (DataIntegrityViolationException e) {           // caught OUTSIDE the tx (Phase D gotcha)
            if (isEmailUniqueViolation(e)) throw emailTaken();  // the race the fast path can't prevent
            throw e;                                            // some other constraint: don't hide it
        }
        return new RegisterResponse(props.requireEmailVerification()
                ? "Verification email sent" : "Account created");
    }

    public TokenResponse login(LoginRequest req) {
        String email = normalize(req.email());
        User user = users.findByEmail(email).orElse(null);

        boolean tooLong = utf8Length(req.password()) > BCRYPT_MAX_BYTES;
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = !tooLong && encoder.matches(req.password(), hashToCheck);

        // Same error for: unknown email, wrong password, deleted account (no account enumeration).
        if (user == null || !passwordOk || user.getDeletedAt() != null) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        // Only a CORRECT password can learn about status (Phase F).
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Please verify your email first");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "This account is suspended");
        }
        return new TokenResponse(jwt.issueAccessToken(user.getId(), user.getRole()), "Bearer", jwt.accessTokenSeconds());
    }

    // ---- helpers ----
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static int utf8Length(String s) { return s.getBytes(StandardCharsets.UTF_8).length; }
    private static ApiException emailTaken() { return ApiException.conflict("EMAIL_TAKEN", "Email is already registered"); }

    private void requireAllowedDomain(String email) {
        var allowed = props.allowedEmailDomains();
        if (allowed.isEmpty()) return;                          // empty list = no restriction (dev only)
        String domain = email.substring(email.lastIndexOf('@') + 1);
        if (!allowed.contains(domain)) {
            throw ApiException.badRequest("EMAIL_DOMAIN_NOT_ALLOWED", "Please use your college email address");
        }
    }

    private static boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return msg != null && msg.contains("uk_users_email");
    }
}