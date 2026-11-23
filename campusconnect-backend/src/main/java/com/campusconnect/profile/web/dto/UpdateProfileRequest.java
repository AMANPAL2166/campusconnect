package com.campusconnect.profile.web.dto;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @Size(max = 500) String bio,
        @Size(max = 100) String department,
        @Min(1) @Max(8) Integer yearOfStudy,
        @Min(2000) @Max(2100) Integer graduationYear,
        @Size(max = 100) String designation) {}