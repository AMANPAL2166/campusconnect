package com.campusconnect.profile.web;

import com.campusconnect.common.exception.ApiException;
import com.campusconnect.common.security.AuthenticatedUser;
import com.campusconnect.profile.api.ProfileDetails;
import com.campusconnect.profile.api.ProfileQueryService;
import com.campusconnect.profile.service.ProfileService;
import com.campusconnect.profile.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileQueryService queries;
    private final ProfileService profileService;

    public ProfileController(ProfileQueryService queries, ProfileService profileService) {
        this.queries = queries;
        this.profileService = profileService;
    }

    @GetMapping("/users/{id}")
    public ProfileResponse get(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser me) {
        ProfileDetails d = queries.getDetails(id)
                .orElseThrow(() -> ApiException.notFound("RESOURCE_NOT_FOUND", "User not found"));
        return ProfileResponse.from(d, me.id());
    }

    @PutMapping("/users/me/profile")
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest req,
                                  @AuthenticationPrincipal AuthenticatedUser me) {
        return ProfileResponse.from(profileService.updateProfile(me, req), me.id());
    }

    @PutMapping("/users/me/skills")
    public SkillsResponse replaceSkills(@Valid @RequestBody UpdateSkillsRequest req,
                                        @AuthenticationPrincipal AuthenticatedUser me) {
        return profileService.replaceSkills(me.id(), req);
    }
}