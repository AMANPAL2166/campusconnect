package com.campusconnect.profile.service;

import com.campusconnect.profile.api.ProfileDetails;
import com.campusconnect.profile.api.ProfileQueryService;
import com.campusconnect.profile.api.ProfileSummary;
import com.campusconnect.profile.domain.Profile;
import com.campusconnect.profile.repository.ProfileRepository;
import com.campusconnect.profile.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
class ProfileQueryServiceImpl implements ProfileQueryService {

    private final ProfileRepository profiles;
    private final UserSkillRepository userSkills;
    private final AvatarUrlResolver avatars;

    ProfileQueryServiceImpl(ProfileRepository profiles, UserSkillRepository userSkills, AvatarUrlResolver avatars) {
        this.profiles = profiles; this.userSkills = userSkills; this.avatars = avatars;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfileDetails> getDetails(long userId) {
        return profiles.findById(userId).map(p -> {
            SkillLists lists = SkillLists.from(userSkills.findRows(userId));
            return new ProfileDetails(p.getUserId(), p.getDisplayName(), p.getBio(), p.getDepartment(),
                    p.getYearOfStudy(), p.getGraduationYear(), p.getDesignation(),
                    avatars.resolve(p.getAvatarKey()), lists.skills(), lists.interests());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProfileSummary> getSummaries(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, ProfileSummary> out = new HashMap<>();
        for (Profile p : profiles.findAllById(new HashSet<>(userIds))) {   // one SELECT ... WHERE user_id IN (...)
            out.put(p.getUserId(), new ProfileSummary(p.getUserId(), p.getDisplayName(),
                    p.getDepartment(), avatars.resolve(p.getAvatarKey())));
        }
        return out;
    }
}