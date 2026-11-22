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

/** Deliberately NOT @Transactional (BCrypt is slow). Each collaborator opens its own short transaction. */
@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RegistrationWriter writer;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final EmailVerificationService verification;
    private final AuthRateLimits limits;
    private final AuthProperties props;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, RegistrationWriter writer, JwtService jwt,
                       RefreshTokenService refreshTokens, EmailVerificationService verification,
                       AuthRateLimits limits, AuthProperties props) {
        this.users = users; this.encoder = encoder; this.writer = writer; this.jwt = jwt;
        this.refreshTokens = refreshTokens; this.verification = verification;
        this.limits = limits; this.props = props;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public RegisterResponse register(RegisterRequest req, String ip) {
        limits.register().acquireOrThrow(ip);
        String email = normalize(req.email());
        requireAllowedDomain(email);
        if (utf8Length(req.password()) > BCRYPT_MAX_BYTES) {
            throw ApiException.badRequest("PASSWORD_TOO_LONG", "Password must be at most 72 bytes");
        }
        if (users.existsByEmail(email)) throw emailTaken();
        String hash = encoder.encode(req.password());
        try {
            writer.create(email, hash, req.displayName().trim());
        } catch (DataIntegrityViolationException e) {
            if (isEmailUniqueViolation(e)) throw emailTaken();
            throw e;
        }
        return new RegisterResponse(props.requireEmailVerification() ? "Verification email sent" : "Account created");
    }

    public AuthResult login(LoginRequest req, String ip) {
        String email = normalize(req.email());
        limits.login().acquireOrThrow(ip);
        limits.loginFailures().failIfExceeded(email);       // cheap check BEFORE the slow BCrypt

        User user = users.findByEmail(email).orElse(null);
        boolean tooLong = utf8Length(req.password()) > BCRYPT_MAX_BYTES;
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = !tooLong && encoder.matches(req.password(), hashToCheck);

        if (user == null || !passwordOk || user.getDeletedAt() != null) {
            limits.loginFailures().recordFailure(email);    // unknown emails count too, so behaviour is identical
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Please verify your email first");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", "This account is suspended");
        }
        return new AuthResult(accessToken(user), refreshTokens.issue(user.getId()));
    }

    public AuthResult refresh(String rawRefreshToken, String ip) {
        limits.refresh().acquireOrThrow(ip);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "Please log in again");
        }
        RefreshTokenService.Rotated rotated = refreshTokens.rotate(rawRefreshToken);

        // Re-read the user: a suspended / deleted user stops here, and a changed role is picked up (JWT staleness fix).
        User user = users.findById(rotated.userId()).orElse(null);
        if (user == null || user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE) {
            refreshTokens.revokeAll(rotated.userId());
            throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "Please log in again");
        }
        return new AuthResult(accessToken(user), rotated.newToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) refreshTokens.revokeByRaw(rawRefreshToken);
    }

    public void logoutAll(long userId) { refreshTokens.revokeAll(userId); }

    public void verifyEmail(String token) { verification.verify(token); }

    /** Always looks the same to the caller (202). The email only goes out for a real, unverified account. */
    public void resendVerification(String rawEmail, String ip) {
        String email = normalize(rawEmail);
        limits.resend().acquireOrThrow(ip);
        limits.resendEmail().acquireOrThrow(email);         // stops mail-bombing one victim from many IPs
        users.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION && u.getDeletedAt() == null)
                .ifPresent(u -> verification.issueAndSend(u.getId(), u.getEmail()));
    }

    // ---- helpers ----
    private TokenResponse accessToken(User u) {
        return new TokenResponse(jwt.issueAccessToken(u.getId(), u.getRole()), "Bearer", jwt.accessTokenSeconds());
    }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static int utf8Length(String s) { return s.getBytes(StandardCharsets.UTF_8).length; }
    private static ApiException emailTaken() { return ApiException.conflict("EMAIL_TAKEN", "Email is already registered"); }

    private void requireAllowedDomain(String email) {
        var allowed = props.allowedEmailDomains();
        if (allowed.isEmpty()) return;
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