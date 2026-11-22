package com.campusconnect.auth.web.dto;
import jakarta.validation.constraints.*;
public record VerifyEmailRequest(@NotBlank @Size(max = 200) String token) {}