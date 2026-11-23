package com.campusconnect.profile.web.dto;

import com.campusconnect.profile.api.ProfileDetails;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Public profile. No email, no role, no internal keys. */
public record ProfileResponse(long id, String displayName, String bio, String department,
                              Integer yearOfStudy, Integer graduationYear, String designation,
                              String avatarUrl, List<String> skills, List<String> interests, Viewer viewer) {

    /** Facts that depend on WHO is asking, kept apart from the content (Phase F, F2). */
    public record Viewer(@JsonProperty("isSelf") boolean self) {}

    public static ProfileResponse from(ProfileDetails d, long viewerId) {
        return new ProfileResponse(d.userId(), d.displayName(), d.bio(), d.department(), d.yearOfStudy(),
                d.graduationYear(), d.designation(), d.avatarUrl(), d.skills(), d.interests(),
                new Viewer(d.userId() == viewerId));
    }
}