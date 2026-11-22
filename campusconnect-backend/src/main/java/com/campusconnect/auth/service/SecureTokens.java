package com.campusconnect.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class SecureTokens {
    private static final SecureRandom RNG = new SecureRandom();
    private SecureTokens() {}

    /** 256 random bits, URL-safe. This is what goes to the user (cookie / email link). */
    static String newToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** This is what goes to the DB. SHA-256 is enough here: a 256-bit random token can't be brute-forced
     *  (BCrypt is for low-entropy human passwords). */
    static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}