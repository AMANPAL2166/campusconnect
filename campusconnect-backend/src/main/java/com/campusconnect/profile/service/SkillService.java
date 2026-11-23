package com.campusconnect.profile.service;

import com.campusconnect.common.exception.ApiException;
import com.campusconnect.profile.domain.Skill;
import com.campusconnect.profile.repository.SkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SkillService {

    private final SkillRepository skills;

    SkillService(SkillRepository skills) { this.skills = skills; }

    /**
     * name -> id, creating missing skills. Must run inside the caller's transaction.
     *
     * ORDER MATTERS: insert first, SELECT after. Under REPEATABLE READ a plain SELECT fixes the
     * transaction's snapshot. If we selected first, a skill committed by a parallel request a moment
     * later would be invisible to our second SELECT, even though our own insert found it as a duplicate.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    Map<String, Long> resolveIds(Collection<String> normalizedNames) {
        if (normalizedNames.isEmpty()) return Map.of();
        for (String name : normalizedNames) skills.insertIfAbsent(name);   // <= 60 tiny statements, rare action
        Map<String, Long> out = new HashMap<>();
        for (Skill s : skills.findByNameIn(normalizedNames)) out.put(s.getName().toLowerCase(Locale.ROOT), s.getId());
        if (out.size() != normalizedNames.size()) throw new IllegalStateException("Skill resolution mismatch");
        return out;
    }

    @Transactional(readOnly = true)
    public List<String> suggest(String rawPrefix, int limit) {
        if (limit < 1 || limit > 20) throw ApiException.badRequest("VALIDATION_FAILED", "limit must be 1 to 20");
        String prefix = SkillNames.normalize(rawPrefix);        // also rejects %, _ and other junk
        return skills.findByNameStartingWithOrderByNameAsc(prefix, PageRequest.of(0, limit))
                .stream().map(Skill::getName).toList();
    }
}