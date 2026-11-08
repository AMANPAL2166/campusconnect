package com.campusconnect.auth.security;

import com.campusconnect.auth.service.JwtService;
import com.campusconnect.common.security.AuthenticatedUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer ...", validates signature + expiry, sets the caller in SecurityContext.
 * It never rejects by itself: it records WHY a token failed, and the entry point turns that into 401.
 * (Created with `new` in SecurityConfig, NOT as a @Component, so it isn't also auto-registered as a servlet filter.)
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = "auth.error";
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip JWT validation for public authentication routes
        return path.startsWith("/api/v1/auth/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthenticatedUser user = jwtService.parse(header.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ExpiredJwtException e) {
                req.setAttribute(AUTH_ERROR_ATTR, "TOKEN_EXPIRED");      // React: try refresh
            } catch (JwtException | IllegalArgumentException e) {
                req.setAttribute(AUTH_ERROR_ATTR, "TOKEN_INVALID");      // React: log in again
            }
        }
        chain.doFilter(req, res);
    }
}