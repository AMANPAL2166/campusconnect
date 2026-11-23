package com.campusconnect.profile.repository;

import com.campusconnect.profile.domain.SkillKind;

public record SkillRow(SkillKind kind, String name) {}