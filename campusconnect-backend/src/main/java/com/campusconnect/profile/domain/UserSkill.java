package com.campusconnect.profile.domain;

import jakarta.persistence.*;

/** Read-only mapping. Rows are written with native "INSERT ... ON DUPLICATE KEY" (see repository). */
@Entity
@Table(name = "user_skills")
public class UserSkill {

    @EmbeddedId
    private UserSkillId id;

    protected UserSkill() {}

    public UserSkillId getId() { return id; }
}