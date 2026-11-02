package com.agentshield.controller;

import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.UpdateAgentRequest;
import com.agentshield.enums.AgentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete Agent Lifecycle: Create -> Get -> Update -> Rotate Key -> Delete")
    void testAgentLifecycle() throws Exception {
        // 1. Create Agent
        CreateAgentRequest createRequest = new CreateAgentRequest("DevAssistant", "Assists with developer tasks");

        MvcResult createResult = mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("DevAssistant")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.apiKey", startsWith("as_live_")))
                .andReturn();

        Map<?, ?> responseMap = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        String agentId = (String) responseMap.get("id");

        // 2. Get Agent by ID
        mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(agentId)))
                .andExpect(jsonPath("$.name", is("DevAssistant")))
                // Ensure apiKeyHash is NOT exposed
                .andExpect(jsonPath("$.apiKeyHash").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist());

        // 3. List Agents
        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 4. Update Agent
        UpdateAgentRequest updateRequest = new UpdateAgentRequest("DevAssistantPro", "Updated description", AgentStatus.DISABLED);

        mockMvc.perform(put("/api/v1/agents/{agentId}", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("DevAssistantPro")))
                .andExpect(jsonPath("$.status", is("DISABLED")));

        // 5. Rotate API Key
        mockMvc.perform(post("/api/v1/agents/{agentId}/rotate-api-key", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey", startsWith("as_live_")));

        // 6. Delete Agent
        mockMvc.perform(delete("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isNoContent());

        // 7. Verify Agent is gone
        mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validation: Reject blank agent name")
    void testCreateAgentValidationFailure() throws Exception {
        CreateAgentRequest invalidRequest = new CreateAgentRequest("", "Some description");

        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))));
    }
}
