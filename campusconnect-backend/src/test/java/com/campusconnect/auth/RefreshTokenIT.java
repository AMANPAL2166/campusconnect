package com.campusconnect.auth;

import com.campusconnect.support.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class RefreshTokenIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    private static final String PW = "correct-horse-battery";
    private record Session(String accessToken, Cookie refresh) {}

    private String newEmail() { return "u" + UUID.randomUUID().toString().substring(0, 8) + "@college.edu"; }

    private MvcResult loginResult(String email, String pw) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("email", email, "password", pw)))).andReturn();
    }

    private Session toSession(MvcResult r) throws Exception {
        String token = om.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
        return new Session(token, r.getResponse().getCookie("refresh_token"));
    }

    private Session newSession(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", PW, "displayName", "Test User"))))
                .andExpect(status().isCreated());
        MvcResult r = loginResult(email, PW);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return toSession(r);
    }

    private ResultActions refresh(Cookie c) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(c));
    }

    @Test
    void login_setsHardenedRefreshCookie() throws Exception {
        String email = newEmail();
        newSession(email);
        String header = loginResult(email, PW).getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("refresh_token=").contains("HttpOnly")
                .contains("SameSite=Strict").contains("Path=/api/v1/auth");
    }

    @Test
    void refresh_rotatesToken_andNewAccessTokenWorks() throws Exception {
        Session s1 = newSession(newEmail());
        Session s2 = toSession(refresh(s1.refresh()).andExpect(status().isOk()).andReturn());

        assertThat(s2.refresh().getValue()).isNotEqualTo(s1.refresh().getValue());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + s2.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void replayingAnOldRefreshToken_isRejected_andKillsTheWholeSession() throws Exception {
        Session s1 = newSession(newEmail());
        Session s2 = toSession(refresh(s1.refresh()).andExpect(status().isOk()).andReturn());

        refresh(s1.refresh()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));   // old token replayed = theft signal
        refresh(s2.refresh()).andExpect(status().isUnauthorized());              // even the newest one is now dead
    }

    @Test
    void parallelRefreshWithSameToken_exactlyOneWins() throws Exception {
        Session s = newSession(newEmail());
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                go.await();
                return refresh(s.refresh()).andReturn().getResponse().getStatus();
            }));
        }
        go.countDown();

        int ok = 0, unauthorized = 0;
        for (Future<Integer> f : results) {
            int code = f.get(30, TimeUnit.SECONDS);
            if (code == 200) ok++;
            if (code == 401) unauthorized++;
        }
        pool.shutdown();

        assertThat(ok).isEqualTo(1);                       // the conditional UPDATE decides, not Java
        assertThat(unauthorized).isEqualTo(threads - 1);
    }

    @Test
    void logout_revokesToken_andIsAlways204() throws Exception {
        Session s = newSession(newEmail());
        mvc.perform(post("/api/v1/auth/logout").cookie(s.refresh())).andExpect(status().isNoContent());
        refresh(s.refresh()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());   // no cookie: still 204
    }

    @Test
    void logoutAll_requiresLogin_andEndsEverySession() throws Exception {
        String email = newEmail();
        Session phone = newSession(email);
        Session laptop = toSession(loginResult(email, PW));

        mvc.perform(post("/api/v1/auth/logout-all")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + phone.accessToken()))
                .andExpect(status().isNoContent());

        refresh(phone.refresh()).andExpect(status().isUnauthorized());
        refresh(laptop.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedUser_cannotRefresh() throws Exception {
        String email = newEmail();
        Session s = newSession(email);
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);
        refresh(s.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyFailedLogins_areRateLimited_evenWithCorrectPasswordAfterwards() throws Exception {
        String email = newEmail();
        newSession(email);
        for (int i = 0; i < 5; i++) {
            assertThat(loginResult(email, "wrong-password-1").getResponse().getStatus()).isEqualTo(401);
        }
        MvcResult blocked = loginResult(email, PW);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(blocked.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getResponse().getContentAsString()).contains("RATE_LIMITED");
    }
}