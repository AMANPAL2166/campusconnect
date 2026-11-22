package com.campusconnect.auth.web.dto;
import jakarta.validation.constraints.*;
public record ResendVerificationRequest(@NotBlank @Email @Size(max = 255) String email) {}