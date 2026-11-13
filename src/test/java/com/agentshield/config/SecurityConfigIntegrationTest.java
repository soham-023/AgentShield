package com.agentshield.config;

import com.agentshield.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    private static final String DEMO_EMAIL = "demo@agentshield.com";
    private static final String DEMO_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Protected API endpoints return a JSON 401 without credentials")
    void apiRequiresAuthentication() throws Exception {
        for (String path : new String[]{"/api/v1/agents", "/api/v1/approvals/pending", "/api/v1/audit-logs",
                "/api/v1/agent-actions"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")))
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.path", is(path)));
        }
        mockMvc.perform(post("/api/v1/agents").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Health, docs and dashboard stay public")
    void publicEndpointsAreOpen() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Agents can submit actions with only their API key (no user login)")
    void agentActionSubmissionIsNotBehindUserAuth() throws Exception {
        String body = "{\"actionType\":\"READ_EMAIL\",\"description\":\"x\"}";
        // Rejected by the service for the bad key (403), not by Spring Security (401)
        mockMvc.perform(post("/api/v1/agent-actions")
                        .header("X-Agent-API-Key", "as_live_not_a_real_key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login returns a JWT that unlocks the API")
    void loginIssuesUsableToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(DEMO_EMAIL, DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andReturn().getResponse().getContentAsString();
        String token = (String) objectMapper.readValue(response, Map.class).get("accessToken");

        mockMvc.perform(get("/api/v1/agents").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Wrong password is a 401 with a generic message")
    void badLoginIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(DEMO_EMAIL, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("A tampered or garbage bearer token is rejected")
    void invalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/agents").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HTTP Basic works for the dashboard")
    void httpBasicIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/agents").with(httpBasic(DEMO_EMAIL, DEMO_PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/agents").with(httpBasic(DEMO_EMAIL, "nope")))
                .andExpect(status().isUnauthorized());
    }
}
