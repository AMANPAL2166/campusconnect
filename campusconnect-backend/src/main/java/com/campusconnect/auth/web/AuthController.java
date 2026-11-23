package com.campusconnect.auth.web;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.auth.service.AuthResult;
import com.campusconnect.auth.service.AuthService;
import com.campusconnect.auth.web.dto.*;
import com.campusconnect.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final AuthProperties props;

    public AuthController(AuthService authService, AuthProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req, http.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return withRefreshCookie(authService.login(req, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String raw, HttpServletRequest http) {
        return withRefreshCookie(authService.refresh(raw, http.getRemoteAddr()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String raw) {
        authService.logout(raw);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser me) {
        authService.logoutAll(me.id());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendVerificationRequest req, HttpServletRequest http) {
        authService.resendVerification(req.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    /** TEMPORARY */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        // AuthenticatedUser ke actual methods ke hisaab se yahan update karein
        return new MeResponse(
                me.id(),
                null, // Agar AuthenticatedUser mein email nahi hai abhi
                false, // Agar emailVerified status available nahi hai
                me.role(),
                null  // Profile service baad mein add hogi
        );
    }

    // ---- cookie helpers ----
    private ResponseEntity<TokenResponse> withRefreshCookie(AuthResult r) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(r.refreshToken(), Duration.ofDays(props.refreshTokenDays())).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(r.tokens());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}