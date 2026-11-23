package com.campusconnect.profile.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long userId;                              // shared PK with users.id (Phase E)

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String department;

    // DB columns are TINYINT / SMALLINT. Hibernate's default for Integer is INTEGER, and
    // ddl-auto=validate would fail with "found tinyint, expecting integer". So we say it explicitly.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(length = 100)
    private String designation;

    @Column(name = "avatar_key", length = 255)
    private String avatarKey;

    protected Profile() {}

    public Profile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    /** One method = one place where "what can change" is visible. userId and avatarKey are NOT editable here. */
    public void update(String displayName, String bio, String department,
                       Integer yearOfStudy, Integer graduationYear, String designation) {
        this.displayName = displayName;
        this.bio = bio;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.graduationYear = graduationYear;
        this.designation = designation;
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getDepartment() { return department; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public Integer getGraduationYear() { return graduationYear; }
    public String getDesignation() { return designation; }
    public String getAvatarKey() { return avatarKey; }
}