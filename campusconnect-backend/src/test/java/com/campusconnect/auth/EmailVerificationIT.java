package com.campusconnect.auth;

import com.campusconnect.support.CapturingEmailSender;
import com.campusconnect.support.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.*;

import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@TestPropertySource(properties = "app.auth.require-email-verification=true")
class EmailVerificationIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;
    @Autowired CapturingEmailSender mail;

    private static final String PW = "correct-horse-battery";

    private String newEmail() { return "u" + UUID.randomUUID().toString().substring(0, 8) + "@college.edu"; }

    private String registerAndGetToken(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", PW, "displayName", "Test User"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Verification email sent"));
        await().atMost(5, SECONDS).until(() -> mail.tokenFor(email).isPresent());   // email is sent async, after commit
        return mail.tokenFor(email).orElseThrow();
    }

    private ResultActions verify(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/verify-email").contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("token", token))));
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("email", email, "password", PW))));
    }

    private ResultActions resend(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/resend-verification").contentType(APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("email", email))));
    }

    @Test
    void unverifiedUserCannotLogin_untilTheyVerify() throws Exception {
        String email = newEmail();
        String token = registerAndGetToken(email);

        login(email).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        verify(token).andExpect(status().isNoContent());
        login(email).andExpect(status().isOk());
    }

    @Test
    void verificationLink_isSingleUse() throws Exception {
        String token = registerAndGetToken(newEmail());
        verify(token).andExpect(status().isNoContent());
        verify(token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    void unknownToken_isRejected() throws Exception {
        verify("this-token-was-never-issued").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        String email = newEmail();
        String token = registerAndGetToken(email);
        jdbc.update("UPDATE email_verification_tokens SET expires_at = DATE_SUB(NOW(6), INTERVAL 2 DAY) "
                + "WHERE user_id = (SELECT id FROM users WHERE email = ?)", email);
        verify(token).andExpect(status().isBadRequest());
    }

    @Test
    void rawTokenIsNeverStoredInTheDatabase() throws Exception {
        String token = registerAndGetToken(newEmail());
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM email_verification_tokens WHERE token_hash = ?", Integer.class, token);
        assertThat(rows).isZero();
    }

    @Test
    void resend_issuesNewLink_killsOldOne_andNeverRevealsWhetherEmailExists() throws Exception {
        String email = newEmail();
        String first = registerAndGetToken(email);

        resend(email).andExpect(status().isAccepted());
        await().atMost(5, SECONDS).until(() -> !first.equals(mail.tokenFor(email).orElse(first)));
        String second = mail.tokenFor(email).orElseThrow();

        verify(first).andExpect(status().isBadRequest());     // old link is dead
        verify(second).andExpect(status().isNoContent());
        resend(newEmail()).andExpect(status().isAccepted());  // unknown email: same 202
    }
}