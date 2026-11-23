package com.campusconnect.profile.service;

import com.campusconnect.common.exception.ApiException;

import java.util.*;
import java.util.regex.Pattern;

/** Skill-name rules in ONE place (Phase F): trim, lowercase, collapse spaces, charset [a-z0-9 +#.-], 1..50 chars. */
final class SkillNames {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9 +#.\\-]{1,50}$");

    private SkillNames() {}

    static String normalize(String raw) {
        String n = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (!VALID.matcher(n).matches()) {
            throw ApiException.badRequest("INVALID_SKILL",
                    "Skill names use letters, digits, spaces and + # . - (max 50 characters)");
        }
        return n;
    }

    /** Normalizes and removes duplicates, keeping the first-seen order. */
    static List<String> normalizeAll(List<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : raw) out.add(normalize(s));
        return new ArrayList<>(out);
    }
}