package com.campusconnect.profile.api;

/** Public contract of the profile module. Auth calls this; it never touches Profile entity or repository. */
public interface ProfileInitializer {
    void createEmpty(long userId, String displayName);
}