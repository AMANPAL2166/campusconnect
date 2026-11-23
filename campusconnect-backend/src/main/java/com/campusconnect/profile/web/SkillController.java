package com.campusconnect.profile.web;

import com.campusconnect.profile.service.SkillService;
import com.campusconnect.profile.web.dto.SkillSuggestionsResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) { this.skillService = skillService; }

    @GetMapping
    public SkillSuggestionsResponse suggest(@RequestParam String prefix,
                                            @RequestParam(defaultValue = "10") int limit) {
        return new SkillSuggestionsResponse(skillService.suggest(prefix, limit));
    }
}