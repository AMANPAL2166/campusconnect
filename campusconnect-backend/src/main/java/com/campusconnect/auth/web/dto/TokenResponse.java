package com.campusconnect.auth.web.dto;
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}