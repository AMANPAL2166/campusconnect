package com.campusconnect.profile.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UpdateSkillsRequest(
        @NotNull @Size(max = 30) List<@NotNull @Size(max = 100) String> skills,
        @NotNull @Size(max = 30) List<@NotNull @Size(max = 100) String> interests) {}