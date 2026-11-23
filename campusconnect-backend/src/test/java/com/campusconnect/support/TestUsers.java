package com.campusconnect.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Needs the "test" profile (email verification off), so register -> login works immediately. */
public class TestUsers {

    public static final String PASSWORD = "correct-horse-battery";

    public record TestUser(String email, long id, String token) {
        public String bearer() { return "Bearer " + token; }
    }

    private final MockMvc mvc;
    private final ObjectMapper om;

    public TestUsers(MockMvc mvc, ObjectMapper om) { this.mvc = mvc; this.om = om; }

    public TestUser registerAndLogin() throws Exception {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@college.edu";
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", PASSWORD, "displayName", "Test User"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    public TestUser login(String email) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = om.readTree(json).get("accessToken").asText();
        String me = mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new TestUser(email, om.readTree(me).get("id").asLong(), token);
    }
}