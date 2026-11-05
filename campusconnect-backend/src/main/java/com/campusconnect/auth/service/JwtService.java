package com.campusconnect.auth.service;

import com.campusconnect.auth.config.JwtProperties;
import com.campusconnect.common.security.AuthenticatedUser;
import com.campusconnect.common.security.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;
    private final String issuer;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {   // HS256 needs >= 256 bits. Fail at startup, not at first login.
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTokenMinutes = props.accessTokenMinutes();
        this.issuer = props.issuer();
    }

    public String issueAccessToken(long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /** Throws ExpiredJwtException (expired) or another JwtException/IllegalArgumentException (invalid). No DB call. */
    public AuthenticatedUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(Long.parseLong(c.getSubject()), Role.valueOf(c.get("role", String.class)));
    }

    public long accessTokenSeconds() { return accessTokenMinutes * 60; }
}