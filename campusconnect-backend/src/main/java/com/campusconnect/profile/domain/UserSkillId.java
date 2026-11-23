package com.campusconnect.profile.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSkillId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)          // VARCHAR + CHECK column, same as Role in Part 1
    @Column(name = "kind", length = 10)
    private SkillKind kind;

    @Column(name = "skill_id")
    private Long skillId;

    protected UserSkillId() {}

    public Long getUserId() { return userId; }
    public SkillKind getKind() { return kind; }
    public Long getSkillId() { return skillId; }

    @Override public boolean equals(Object o) {
        return o instanceof UserSkillId x && Objects.equals(userId, x.userId)
                && kind == x.kind && Objects.equals(skillId, x.skillId);
    }
    @Override public int hashCode() { return Objects.hash(userId, kind, skillId); }
}