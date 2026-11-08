package com.campusconnect.auth.service;

import com.campusconnect.auth.config.AuthProperties;
import com.campusconnect.auth.domain.User;
import com.campusconnect.auth.domain.UserStatus;
import com.campusconnect.auth.repository.UserRepository;
import com.campusconnect.common.security.Role;
import com.campusconnect.profile.api.ProfileInitializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Separate bean so @Transactional really applies (Phase D: self-invocation trap). */
@Component
class RegistrationWriter {

    private final UserRepository users;
    private final ProfileInitializer profiles;
    private final AuthProperties props;

    RegistrationWriter(UserRepository users, ProfileInitializer profiles, AuthProperties props) {
        this.users = users;
        this.profiles = profiles;
        this.props = props;
    }

    @Transactional
    void create(String email, String passwordHash, String displayName) {
        boolean verified = !props.requireEmailVerification();
        User user = new User(email, passwordHash, Role.STUDENT,           // everyone registers as STUDENT (Phase F)
                verified ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION,
                verified ? Instant.now() : null);
        users.saveAndFlush(user);                      // INSERT happens now; a duplicate email throws here
        profiles.createEmpty(user.getId(), displayName);   // same transaction: both rows or neither
    }
}