package com.campusconnect.auth.web.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 128) String password) {}