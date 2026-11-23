package com.campusconnect.profile.service;

import com.campusconnect.common.exception.ApiException;
import com.campusconnect.common.security.AuthenticatedUser;
import com.campusconnect.common.security.Role;
import com.campusconnect.profile.api.ProfileDetails;
import com.campusconnect.profile.api.ProfileQueryService;
import com.campusconnect.profile.domain.SkillKind;
import com.campusconnect.profile.repository.ProfileRepository;
import com.campusconnect.profile.repository.UserSkillRepository;
import com.campusconnect.profile.web.dto.UpdateProfileRequest;
import com.campusconnect.profile.web.dto.UpdateSkillsRequest;
import com.campusconnect.profile.web.dto.SkillsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final UserSkillRepository userSkills;
    private final SkillService skillService;
    private final ProfileQueryService queries;

    ProfileService(ProfileRepository profiles, UserSkillRepository userSkills,
                   SkillService skillService, ProfileQueryService queries) {
        this.profiles = profiles; this.userSkills = userSkills; this.skillService = skillService; this.queries = queries;
    }

    /** The target is ALWAYS the caller (me.id() from the JWT). There is no id in the URL, so IDOR is impossible by design. */
    @Transactional
    public ProfileDetails updateProfile(AuthenticatedUser me, UpdateProfileRequest r) {
        String name = r.displayName().trim();
        if (name.length() < 2) throw ApiException.badRequest("VALIDATION_FAILED", "displayName must be at least 2 characters");

        Integer year = r.yearOfStudy();
        String designation = blankToNull(r.designation());
        // Role comes from the JWT: at most 15 minutes stale (known trade-off, Phase D).
        if (year != null && me.role() != Role.STUDENT) {
            throw ApiException.badRequest("YEAR_ONLY_FOR_STUDENTS", "yearOfStudy is only for students");
        }
        if (designation != null && me.role() == Role.STUDENT) {
            throw ApiException.badRequest("DESIGNATION_ONLY_FOR_FACULTY", "designation is only for faculty");
        }

        var profile = profiles.findById(me.id())
                .orElseThrow(() -> ApiException.notFound("RESOURCE_NOT_FOUND", "Profile not found"));
        profile.update(name, blankToNull(r.bio()), blankToNull(r.department()), year, r.graduationYear(), designation);
        return queries.getDetails(me.id()).orElseThrow();      // same transaction: returns what we just saved
    }

    @Transactional
    public SkillsResponse replaceSkills(long userId, UpdateSkillsRequest req) {
        List<String> skills = SkillNames.normalizeAll(req.skills());
        List<String> interests = SkillNames.normalizeAll(req.interests());

        Set<String> all = new LinkedHashSet<>(skills);
        all.addAll(interests);
        Map<String, Long> ids = skillService.resolveIds(all);   // insert-then-select, see SkillService

        sync(userId, SkillKind.SKILL, toIds(skills, ids));
        sync(userId, SkillKind.INTEREST, toIds(interests, ids));

        SkillLists lists = SkillLists.from(userSkills.findRows(userId));
        return new SkillsResponse(lists.skills(), lists.interests());
    }

    /** Diff, not delete-all-then-insert: fewer writes, and a retry or double click changes nothing. */
    private void sync(long userId, SkillKind kind, Set<Long> desired) {
        Set<Long> current = new HashSet<>(userSkills.findSkillIds(userId, kind));
        Set<Long> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);
        if (!toRemove.isEmpty()) userSkills.deleteSkills(userId, kind, toRemove);
        for (Long id : desired) {
            if (!current.contains(id)) userSkills.insertIfAbsent(userId, id, kind.name());
        }
    }

    private static Set<Long> toIds(List<String> names, Map<String, Long> ids) {
        Set<Long> out = new HashSet<>();
        for (String n : names) out.add(ids.get(n));
        return out;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}