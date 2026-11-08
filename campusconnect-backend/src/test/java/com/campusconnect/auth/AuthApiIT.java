package com.campusconnect.auth;

import com.campusconnect.support.MySqlContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlContainerConfig.class)
class AuthApiIT {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper om;

    private static final String PASSWORD = "correct-horse-battery";

    private String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@college.edu";
    }

    private ResultActions register(String email, String password) throws Exception {
        String body = om.writeValueAsString(Map.of("email", email, "password", password, "displayName", "Test User"));
        return mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = om.writeValueAsString(Map.of("email", email, "password", password));
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(body));
    }

    private int countUsers(String email) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    }

    @Test
    void register_createsUserAndProfile_withHashedPassword() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());

        assertThat(countUsers(email)).isEqualTo(1);
        Integer profiles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM profiles p JOIN users u ON u.id = p.user_id WHERE u.email = ?", Integer.class, email);
        assertThat(profiles).isEqualTo(1);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void register_duplicateEmail_isConflict_evenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());
        register(email.toUpperCase(), PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void register_wrongDomain_isRejected() throws Exception {
        register("someone@gmail.com", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    @Test
    void register_invalidBody_returnsFieldErrors() throws Exception {
        register("not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_passwordOver72Bytes_isRejectedNotTruncated() throws Exception {
        String longPassword = "🙂".repeat(30);   // 60 chars but 120 bytes: passes @Size, fails the byte check
        register(uniqueEmail(), longPassword)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_LONG"));
    }

    @Test
    void login_thenCallProtectedEndpoint_works() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());

        String json = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String token = om.readTree(json).get("accessToken").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void login_wrongPassword_and_unknownEmail_lookIdentical() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());

        login(email, "wrong-password-123").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpoint_withoutOrWithBadToken_is401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void concurrentRegistrationsOfSameEmail_exactlyOneWins() throws Exception {
        String email = uniqueEmail();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);

        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGun.await();                                   // all threads fire together
                return register(email, PASSWORD).andReturn().getResponse().getStatus();
            }));
        }
        startGun.countDown();

        int created = 0, conflicts = 0;
        for (Future<Integer> f : results) {
            int s = f.get(30, TimeUnit.SECONDS);
            if (s == 201) created++;
            if (s == 409) conflicts++;
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(conflicts).isEqualTo(threads - 1);
        assertThat(countUsers(email)).isEqualTo(1);                 // the DB, not Java, is the referee
    }
}