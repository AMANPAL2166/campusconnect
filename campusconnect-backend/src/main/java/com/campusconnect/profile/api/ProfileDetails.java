package com.campusconnect.profile.api;

import java.util.List;

public record ProfileDetails(long userId, String displayName, String bio, String department,
                             Integer yearOfStudy, Integer graduationYear, String designation,
                             String avatarUrl, List<String> skills, List<String> interests) {}