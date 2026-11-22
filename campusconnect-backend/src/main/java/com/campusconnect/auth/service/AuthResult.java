package com.campusconnect.auth.service;

import com.campusconnect.auth.web.dto.TokenResponse;

/** Access token goes in the JSON body; the refresh token is delivered as a cookie by the controller. */
public record AuthResult(TokenResponse tokens, String refreshToken) {}