package com.agentshield.controller;

import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.CreatePolicyRequest;
import com.agentshield.dto.request.SubmitActionRequest;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.RequestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "demo@agentshield.com")
@AutoConfigureMockMvc
class FullWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Workflow: Agent Registration -> Policies -> Allow / Deny / Require Approval -> Human Decision -> Audit")
    void testCompleteAgentShieldWorkflow() throws Exception {
        // ===============================================================
        // 1. REGISTER AN AGENT
        // ===============================================================
        CreateAgentRequest agentReq = new CreateAgentRequest("ProfBot", "Assistant for professor tasks");
        MvcResult agentResult = mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey", startsWith("as_live_")))
                .andReturn();

        Map<?, ?> agentMap = objectMapper.readValue(agentResult.getResponse().getContentAsString(), Map.class);
        String agentId = (String) agentMap.get("id");
        String apiKey = (String) agentMap.get("apiKey");

        // ===============================================================
        // 2. CONFIGURE PERMISSION POLICIES
        //    READ_EMAIL  -> ALLOW
        //    SEND_EMAIL  -> REQUIRE_APPROVAL
        //    DELETE_FILE -> DENY
        // ===============================================================
        mockMvc.perform(post("/api/v1/agents/{agentId}/policies", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePolicyRequest(ActionType.READ_EMAIL, PermissionDecision.ALLOW))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision", is("ALLOW")));

        mockMvc.perform(post("/api/v1/agents/{agentId}/policies", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePolicyRequest(ActionType.SEND_EMAIL, PermissionDecision.REQUIRE_APPROVAL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision", is("REQUIRE_APPROVAL")));

        mockMvc.perform(post("/api/v1/agents/{agentId}/policies", agentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePolicyRequest(ActionType.DELETE_FILE, PermissionDecision.DENY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision", is("DENY")));

        // ===============================================================
        // 3. ACTION 1: READ_EMAIL (Policy = ALLOW) -> Executes automatically!
        // ===============================================================
        SubmitActionRequest readEmailAction = SubmitActionRequest.builder()
                .actionType(ActionType.READ_EMAIL)
                .description("Read inbox messages")
                .parameters(Map.of())
                .build();

        mockMvc.perform(post("/api/v1/agent-actions")
                        .header("X-Agent-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(readEmailAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("ALLOW")))
                .andExpect(jsonPath("$.status", is("EXECUTED")))
                .andExpect(jsonPath("$.executionResult", containsString("SIMULATED")));

        // ===============================================================
        // 4. ACTION 2: DELETE_FILE (Policy = DENY) -> Blocked immediately!
        // ===============================================================
        SubmitActionRequest deleteFileAction = SubmitActionRequest.builder()
                .actionType(ActionType.DELETE_FILE)
                .description("Delete database backup")
                .parameters(Map.of("target", "backup.sql"))
                .build();

        mockMvc.perform(post("/api/v1/agent-actions")
                        .header("X-Agent-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteFileAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("DENY")))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // ===============================================================
        // 5. ACTION 3: EXECUTE_REPORT (No policy defined) -> DEFAULT DENY!
        // ===============================================================
        SubmitActionRequest reportAction = SubmitActionRequest.builder()
                .actionType(ActionType.EXECUTE_REPORT)
                .description("Run annual grade report")
                .parameters(Map.of())
                .build();

        mockMvc.perform(post("/api/v1/agent-actions")
                        .header("X-Agent-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("DENY")))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // ===============================================================
        // 6. ACTION 4: SEND_EMAIL (Policy = REQUIRE_APPROVAL) -> Pending!
        // ===============================================================
        SubmitActionRequest sendEmailAction = SubmitActionRequest.builder()
                .actionType(ActionType.SEND_EMAIL)
                .description("Send email to professor")
                .parameters(Map.of("to", "dean@college.edu", "body", "Here is my final project."))
                .build();

        MvcResult pendingResult = mockMvc.perform(post("/api/v1/agent-actions")
                        .header("X-Agent-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision", is("REQUIRE_APPROVAL")))
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
                .andReturn();

        Map<?, ?> pendingMap = objectMapper.readValue(pendingResult.getResponse().getContentAsString(), Map.class);
        String pendingRequestId = (String) pendingMap.get("id");

        // ===============================================================
        // 7. HUMAN CHECKS PENDING APPROVAL QUEUE
        // ===============================================================
        mockMvc.perform(get("/api/v1/approvals/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.id == '" + pendingRequestId + "')].status", contains("PENDING_APPROVAL")));

        // ===============================================================
        // 8. HUMAN APPROVES THE REQUEST -> Executes!
        // ===============================================================
        mockMvc.perform(post("/api/v1/approvals/{requestId}/approve", pendingRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comments\": \"Approved by professor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EXECUTED")))
                .andExpect(jsonPath("$.executionResult", containsString("SIMULATED")));

        // ===============================================================
        // 9. VERIFY AUDIT LOGS
        // ===============================================================
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(5))));
    }
}
