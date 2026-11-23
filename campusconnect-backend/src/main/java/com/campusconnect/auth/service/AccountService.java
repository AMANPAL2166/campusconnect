package com.campusconnect.auth.service;

import com.campusconnect.auth.repository.UserRepository;
import com.campusconnect.auth.web.dto.MeResponse;
import com.campusconnect.common.exception.ApiException;
import com.campusconnect.profile.api.ProfileQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository users;
    private final ProfileQueryService profiles;

    public AccountService(UserRepository users, ProfileQueryService profiles) {
        this.users = users; this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public MeResponse me(long userId) {
        var u = users.findById(userId).filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("RESOURCE_NOT_FOUND", "Account not found"));
        var profile = profiles.getDetails(userId)
                .orElseThrow(() -> ApiException.notFound("RESOURCE_NOT_FOUND", "Profile not found"));
        // Role is read from the DB here (fresh), not from the JWT.
        return new MeResponse(u.getId(), u.getEmail(), u.getEmailVerifiedAt() != null, u.getRole(), profile);
    }
}