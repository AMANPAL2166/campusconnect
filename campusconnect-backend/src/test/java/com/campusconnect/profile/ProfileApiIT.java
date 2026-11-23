package com.campusconnect.profile;

import com.campusconnect.support.IntegrationTest;
import com.campusconnect.support.TestUsers;
import com.campusconnect.support.TestUsers.TestUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProfileApiIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    TestUsers users;

    @BeforeEach
    void setUp() { users = new TestUsers(mvc, om); }

    private ResultActions getAs(String url, TestUser u) throws Exception {
        return mvc.perform(get(url).header("Authorization", u.bearer()));
    }

    private ResultActions putAs(String url, TestUser u, Object body) throws Exception {
        return mvc.perform(put(url).header("Authorization", u.bearer())
                .contentType(APPLICATION_JSON).content(om.writeValueAsString(body)));
    }

    private int countUserSkills(long userId, String kind) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_skills WHERE user_id = ? AND kind = ?",
                Integer.class, userId, kind);
    }

    // ---------- reading ----------

    @Test
    void me_returnsEmailRoleAndProfile() throws Exception {
        TestUser u = users.registerAndLogin();
        getAs("/api/v1/users/me", u).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(u.email()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.profile.displayName").value("Test User"))
                .andExpect(jsonPath("$.profile.skills").isEmpty());
    }

    @Test
    void publicProfile_neverExposesEmailOrRole_andShowsViewerFlag() throws Exception {
        TestUser a = users.registerAndLogin();
        TestUser b = users.registerAndLogin();

        getAs("/api/v1/users/" + a.id(), b).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.viewer.isSelf").value(false));
        getAs("/api/v1/users/" + a.id(), a).andExpect(jsonPath("$.viewer.isSelf").value(true));
    }

    @Test
    void unknownUser_is404_andNoToken_is401() throws Exception {
        TestUser u = users.registerAndLogin();
        getAs("/api/v1/users/999999999", u).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/v1/users/" + u.id())).andExpect(status().isUnauthorized());
    }

    // ---------- editing profile ----------

    @Test
    void updateProfile_savesTrimmedValues_andNeverTouchesAnotherUser() throws Exception {
        TestUser a = users.registerAndLogin();
        TestUser b = users.registerAndLogin();

        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "  Asha Verma ", "bio", "Backend + ML",
                "department", "CSE", "yearOfStudy", 3, "graduationYear", 2027))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha Verma"))
                .andExpect(jsonPath("$.yearOfStudy").value(3))
                .andExpect(jsonPath("$.viewer.isSelf").value(true));

        getAs("/api/v1/users/" + b.id(), a)                       // b is unchanged
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.bio").value(nullValue()));
    }

    @Test
    void updateProfile_isFullReplace_missingFieldsBecomeNull() throws Exception {
        TestUser a = users.registerAndLogin();
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "Asha", "bio", "hello")).andExpect(status().isOk());
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "Asha")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value(nullValue()));
    }

    @Test
    void updateProfile_validationErrors() throws Exception {
        TestUser a = users.registerAndLogin();
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "   ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", " a ")).andExpect(status().isBadRequest());
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "Asha", "yearOfStudy", 9))
                .andExpect(status().isBadRequest());
    }

    @Test
    void student_cannotSetDesignation() throws Exception {
        TestUser a = users.registerAndLogin();
        putAs("/api/v1/users/me/profile", a, Map.of("displayName", "Asha", "designation", "Professor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DESIGNATION_ONLY_FOR_FACULTY"));
    }

    @Test
    void faculty_canSetDesignation_butNotYearOfStudy() throws Exception {
        TestUser f = users.registerAndLogin();
        jdbc.update("UPDATE users SET role = 'FACULTY' WHERE id = ?", f.id());
        TestUser faculty = users.login(f.email());                // new JWT carries the new role; the old one still says STUDENT

        putAs("/api/v1/users/me/profile", faculty, Map.of("displayName", "Dr Rao", "designation", "Professor"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.designation").value("Professor"));
        putAs("/api/v1/users/me/profile", faculty, Map.of("displayName", "Dr Rao", "yearOfStudy", 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("YEAR_ONLY_FOR_STUDENTS"));
    }

    // ---------- skills ----------

    @Test
    void putSkills_normalizes_replaces_andIsIdempotent() throws Exception {
        TestUser a = users.registerAndLogin();
        Map<String, Object> body = Map.of("skills", List.of("Java", " Spring   Boot ", "C++", "java"),
                "interests", List.of("Machine Learning", "java"));

        for (int i = 0; i < 2; i++) {                             // same request twice: same result
            putAs("/api/v1/users/me/skills", a, body).andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills[0]").value("c++"))
                    .andExpect(jsonPath("$.skills[1]").value("java"))
                    .andExpect(jsonPath("$.skills[2]").value("spring boot"))
                    .andExpect(jsonPath("$.interests", hasItems("machine learning", "java")));
        }
        assertThat(countUserSkills(a.id(), "SKILL")).isEqualTo(3);
        assertThat(countUserSkills(a.id(), "INTEREST")).isEqualTo(2);

        putAs("/api/v1/users/me/skills", a, Map.of("skills", List.of("python"), "interests", List.of()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.skills[0]").value("python"));
        assertThat(countUserSkills(a.id(), "SKILL")).isEqualTo(1);       // replaced, not appended
        assertThat(countUserSkills(a.id(), "INTEREST")).isZero();

        getAs("/api/v1/users/" + a.id(), a).andExpect(jsonPath("$.skills[0]").value("python"));
    }

    @Test
    void putSkills_rejectsBadNames_andTooMany() throws Exception {
        TestUser a = users.registerAndLogin();
        putAs("/api/v1/users/me/skills", a, Map.of("skills", List.of("drop table;"), "interests", List.of()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SKILL"));

        List<String> thirtyOne = new ArrayList<>();
        for (int i = 0; i < 31; i++) thirtyOne.add("skill" + i);
        putAs("/api/v1/users/me/skills", a, Map.of("skills", thirtyOne, "interests", List.of()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownSkill_isAddedToTheVocabulary() throws Exception {
        TestUser a = users.registerAndLogin();
        String fresh = "zz-" + UUID.randomUUID().toString().substring(0, 8);
        putAs("/api/v1/users/me/skills", a, Map.of("skills", List.of(fresh), "interests", List.of()))
                .andExpect(status().isOk());
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM skills WHERE name = ?", Integer.class, fresh);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void tenUsersCreatingTheSameNewSkillAtOnce_yieldExactlyOneSkillRow() throws Exception {
        String fresh = "zz-" + UUID.randomUUID().toString().substring(0, 8);
        int n = 10;
        List<TestUser> people = new ArrayList<>();
        for (int i = 0; i < n; i++) people.add(users.registerAndLogin());

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (TestUser p : people) {
            results.add(pool.submit(() -> {
                go.await();
                return putAs("/api/v1/users/me/skills", p, Map.of("skills", List.of(fresh), "interests", List.of()))
                        .andReturn().getResponse().getStatus();
            }));
        }
        go.countDown();
        for (Future<Integer> f : results) assertThat(f.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        pool.shutdown();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM skills WHERE name = ?", Integer.class, fresh)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_skills us JOIN skills s ON s.id = us.skill_id WHERE s.name = ?",
                Integer.class, fresh)).isEqualTo(n);                 // every user got it, none lost
    }

    // ---------- autocomplete ----------

    @Test
    void autocomplete_returnsPrefixMatches() throws Exception {
        TestUser a = users.registerAndLogin();
        getAs("/api/v1/skills?prefix=rea", a).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasItems("react", "react native")));
        getAs("/api/v1/skills?prefix=REA&limit=1", a).andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void autocomplete_rejectsWildcardsAndBadLimit() throws Exception {
        TestUser a = users.registerAndLogin();
        getAs("/api/v1/skills?prefix=%25", a).andExpect(status().isBadRequest())      // "%" must not dump the table
                .andExpect(jsonPath("$.code").value("INVALID_SKILL"));
        getAs("/api/v1/skills?prefix=rea&limit=0", a).andExpect(status().isBadRequest());
        getAs("/api/v1/skills?prefix=rea&limit=21", a).andExpect(status().isBadRequest());
    }
}