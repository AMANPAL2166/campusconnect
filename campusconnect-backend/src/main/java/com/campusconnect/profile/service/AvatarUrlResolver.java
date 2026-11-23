package com.campusconnect.profile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Turns a storage key into a public URL. Returns null until the storage branch sets a base URL. */
@Component
class AvatarUrlResolver {

    private final String base;

    AvatarUrlResolver(@Value("${app.storage.public-base-url:}") String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    String resolve(String key) {
        return (key == null || key.isBlank() || base.isBlank()) ? null : base + "/" + key;
    }
}