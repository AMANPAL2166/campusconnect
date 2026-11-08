package com.campusconnect.profile.service;

import com.campusconnect.profile.api.ProfileInitializer;
import com.campusconnect.profile.domain.Profile;
import com.campusconnect.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProfileInitializerImpl implements ProfileInitializer {

    private final ProfileRepository profiles;

    ProfileInitializerImpl(ProfileRepository profiles) { this.profiles = profiles; }

    // MANDATORY: must run inside the caller's transaction, so user + profile are atomic (Phase D).
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void createEmpty(long userId, String displayName) {
        profiles.save(new Profile(userId, displayName));
    }
}