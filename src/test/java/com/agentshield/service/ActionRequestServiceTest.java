package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.SubmitActionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.Agent;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.AuditEventType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.RequestStatus;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.BadRequestException;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.executor.SimulatedActionExecutor;
import com.agentshield.repository.ActionRequestRepository;
import com.agentshield.repository.AgentRepository;
import com.agentshield.repository.ExecutionRecordRepository;
import com.agentshield.util.ApiKeyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionRequestServiceTest {

    private static final String RAW_KEY = "as_live_test-key";

    @Mock
    private ActionRequestRepository actionRequestRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private SimulatedActionExecutor actionExecutor;

    @Mock
    private AuditService auditService;

    @Mock
    private ExecutionRecordRepository executionRecordRepository;

    @InjectMocks
    private ActionRequestService actionRequestService;

    private Agent agent;
    private SubmitActionRequest request;

    @BeforeEach
    void setUp() {
        agent = Agent.builder().id(UUID.randomUUID()).name("Mail Agent")
                .apiKeyHash(ApiKeyUtils.hashApiKey(RAW_KEY)).status(AgentStatus.ACTIVE).build();
        request = SubmitActionRequest.builder()
                .actionType(ActionType.SEND_EMAIL)
                .description("Send weekly report")
                .build();
    }

    private void agentFound() {
        when(agentRepository.findByApiKeyHash(ApiKeyUtils.hashApiKey(RAW_KEY))).thenReturn(Optional.of(agent));
    }

    private void saveReturnsWithId() {
        when(actionRequestRepository.save(any(ActionRequest.class))).thenAnswer(inv -> {
            ActionRequest saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    @Test
    @DisplayName("A missing API key is rejected with a message naming the header")
    void missingApiKeyIsRejected() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> actionRequestService.processActionRequest(null, request));
        assertTrue(ex.getMessage().contains("X-Agent-API-Key"));
        verifyNoInteractions(agentRepository, actionRequestRepository);
    }

    @Test
    @DisplayName("A blank API key is treated as missing")
    void blankApiKeyIsRejected() {
        assertThrows(AccessDeniedException.class,
                () -> actionRequestService.processActionRequest("   ", request));
        verifyNoInteractions(agentRepository);
    }

    @Test
    @DisplayName("An API key that matches no agent is rejected")
    void unknownApiKeyIsRejected() {
        when(agentRepository.findByApiKeyHash(anyString())).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> actionRequestService.processActionRequest("as_live_unknown", request));
        assertEquals("Invalid Agent API key", ex.getMessage());
        verifyNoInteractions(actionRequestRepository, policyService);
    }

    @Test
    @DisplayName("Surrounding whitespace in the API key is trimmed before hashing")
    void apiKeyIsTrimmedBeforeLookup() {
        agentFound();
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL)).thenReturn(PermissionDecision.DENY);
        saveReturnsWithId();

        actionRequestService.processActionRequest("  " + RAW_KEY + "  ", request);

        verify(agentRepository).findByApiKeyHash(ApiKeyUtils.hashApiKey(RAW_KEY));
    }

    @Test
    @DisplayName("A disabled agent cannot submit actions")
    void disabledAgentIsRejected() {
        agent.setStatus(AgentStatus.DISABLED);
        agentFound();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> actionRequestService.processActionRequest(RAW_KEY, request));
        assertTrue(ex.getMessage().contains("DISABLED"));
        verifyNoInteractions(policyService, actionRequestRepository, auditService);
    }

    @Test
    @DisplayName("ALLOW executes the action at once and audits ACTION_EXECUTED")
    void allowedActionIsExecuted() {
        agentFound();
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL)).thenReturn(PermissionDecision.ALLOW);
        saveReturnsWithId();
        when(actionExecutor.execute(any(ActionRequest.class)))
                .thenReturn(ExecutionRecord.builder().result("Email sent").build());

        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertEquals(PermissionDecision.ALLOW, response.getDecision());
        assertEquals("Email sent", response.getExecutionResult());
        verify(actionExecutor).execute(any(ActionRequest.class));
        verify(auditService).recordEvent(eq(agent.getId()), eq(agent.getId()), eq(response.getId()),
                eq(AuditEventType.ACTION_EXECUTED), eq("ALLOWED_AND_EXECUTED"), anyMap());
    }
}
