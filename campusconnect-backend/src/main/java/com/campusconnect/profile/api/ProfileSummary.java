package com.campusconnect.profile.api;

/** Small card shown on posts, comments, lists. */
public record ProfileSummary(long id, String displayName, String department, String avatarUrl) {}