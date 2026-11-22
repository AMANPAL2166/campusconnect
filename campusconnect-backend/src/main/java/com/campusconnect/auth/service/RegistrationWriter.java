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

@Component
class RegistrationWriter {

    private final UserRepository users;
    private final ProfileInitializer profiles;
    private final EmailVerificationService verification;
    private final AuthProperties props;

    RegistrationWriter(UserRepository users, ProfileInitializer profiles,
                       EmailVerificationService verification, AuthProperties props) {
        this.users = users; this.profiles = profiles; this.verification = verification; this.props = props;
    }

    @Transactional
    void create(String email, String passwordHash, String displayName) {
        boolean verified = !props.requireEmailVerification();
        User user = new User(email, passwordHash, Role.STUDENT,
                verified ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION,
                verified ? Instant.now() : null);
        users.saveAndFlush(user);
        profiles.createEmpty(user.getId(), displayName);
        if (!verified) {
            verification.issueAndSend(user.getId(), email);   // same tx: token row is atomic with the user
        }
    }
}