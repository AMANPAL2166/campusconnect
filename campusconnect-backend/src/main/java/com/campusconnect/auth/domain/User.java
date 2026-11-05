package com.campusconnect.auth.domain;

import com.campusconnect.common.security.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // @JdbcTypeCode: newer Hibernate expects a native MySQL ENUM column for EnumType.STRING.
    // Our column is VARCHAR + CHECK (Phase E), so we say so, otherwise ddl-auto=validate fails.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 25)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {}   // JPA needs this

    public User(String email, String passwordHash, Role role, UserStatus status, Instant emailVerifiedAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof User u && id != null && id.equals(u.id));
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}