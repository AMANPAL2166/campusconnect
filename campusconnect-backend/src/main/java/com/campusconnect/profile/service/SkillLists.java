package com.campusconnect.profile.service;

import com.campusconnect.profile.domain.SkillKind;
import com.campusconnect.profile.repository.SkillRow;

import java.util.ArrayList;
import java.util.List;

record SkillLists(List<String> skills, List<String> interests) {
    static SkillLists from(List<SkillRow> rows) {
        List<String> skills = new ArrayList<>(), interests = new ArrayList<>();
        for (SkillRow r : rows) (r.kind() == SkillKind.SKILL ? skills : interests).add(r.name());
        return new SkillLists(skills, interests);
    }
}