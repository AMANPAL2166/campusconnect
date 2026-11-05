package com.campusconnect.profile.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long userId;                 // shared PK with users.id (Phase E)

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    protected Profile() {}

    public Profile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
}