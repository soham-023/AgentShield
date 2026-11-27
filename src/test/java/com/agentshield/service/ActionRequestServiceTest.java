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

    @Test
    @DisplayName("DENY rejects the action without executing it")
    void deniedActionIsNotExecuted() {
        agentFound();
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL)).thenReturn(PermissionDecision.DENY);
        saveReturnsWithId();

        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertEquals(RequestStatus.REJECTED, response.getStatus());
        assertNull(response.getExecutionResult());
        verifyNoInteractions(actionExecutor);
        verify(auditService).recordEvent(any(), any(), any(),
                eq(AuditEventType.ACTION_DENIED), eq("DENIED_BY_POLICY"), anyMap());
    }

    @Test
    @DisplayName("REQUIRE_APPROVAL parks the request and sets its expiry")
    void approvalRequiredActionIsParked() {
        agentFound();
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL))
                .thenReturn(PermissionDecision.REQUIRE_APPROVAL);
        saveReturnsWithId();

        LocalDateTime before = LocalDateTime.now();
        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertEquals(RequestStatus.PENDING_APPROVAL, response.getStatus());
        assertNotNull(response.getExpiresAt());
        assertFalse(response.getExpiresAt().isBefore(before.plusHours(24)));
        assertTrue(response.getExpiresAt().isBefore(before.plusHours(25)));
        verifyNoInteractions(actionExecutor);
        verify(auditService).recordEvent(any(), any(), any(),
                eq(AuditEventType.ACTION_PENDING_APPROVAL), eq("PENDING_HUMAN_APPROVAL"), anyMap());
    }

    @Test
    @DisplayName("A repeated idempotency key returns the existing request")
    void repeatedIdempotencyKeyReturnsExistingRequest() {
        agentFound();
        ActionRequest existing = ActionRequest.builder().id(UUID.randomUUID()).agent(agent)
                .actionType(ActionType.SEND_EMAIL).status(RequestStatus.EXECUTED)
                .decision(PermissionDecision.ALLOW).idempotencyKey("retry-1").build();
        when(actionRequestRepository.findByAgent_IdAndIdempotencyKey(agent.getId(), "retry-1"))
                .thenReturn(Optional.of(existing));
        when(executionRecordRepository.findByActionRequestId(existing.getId()))
                .thenReturn(Optional.of(ExecutionRecord.builder().result("Email sent").build()));
        request.setIdempotencyKey("  retry-1 ");

        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertEquals(existing.getId(), response.getId());
        assertEquals("Email sent", response.getExecutionResult());
        verify(actionRequestRepository, never()).save(any());
        verifyNoInteractions(policyService, actionExecutor, auditService);
    }

    @Test
    @DisplayName("An idempotency key used by another agent does not reuse its request")
    void idempotencyKeyIsScopedToAgent() {
        agentFound();
        request.setIdempotencyKey("shared-key");
        when(actionRequestRepository.findByAgent_IdAndIdempotencyKey(agent.getId(), "shared-key"))
                .thenReturn(Optional.empty());
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL)).thenReturn(PermissionDecision.DENY);
        saveReturnsWithId();

        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertEquals(RequestStatus.REJECTED, response.getStatus());
        verify(actionRequestRepository).findByAgent_IdAndIdempotencyKey(agent.getId(), "shared-key");
        verify(actionRequestRepository).save(any(ActionRequest.class));
    }

    @Test
    @DisplayName("Without an idempotency key a random one is generated")
    void missingIdempotencyKeyIsGenerated() {
        agentFound();
        when(policyService.evaluatePermission(agent, ActionType.SEND_EMAIL)).thenReturn(PermissionDecision.DENY);
        saveReturnsWithId();

        ActionRequestResponse response = actionRequestService.processActionRequest(RAW_KEY, request);

        assertDoesNotThrow(() -> UUID.fromString(response.getIdempotencyKey()));
    }

    @Test
    @DisplayName("Looking up an unknown request id gives 404")
    void unknownRequestIdIsNotFound() {
        UUID id = UUID.randomUUID();
        when(actionRequestRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> actionRequestService.getRequestById(id));
    }

    @Test
    @DisplayName("Listing requests attaches each request's execution result")
    void listingIncludesExecutionResults() {
        ActionRequest executed = ActionRequest.builder().id(UUID.randomUUID()).agent(agent)
                .actionType(ActionType.READ_FILE).status(RequestStatus.EXECUTED).idempotencyKey("k1").build();
        ActionRequest rejected = ActionRequest.builder().id(UUID.randomUUID()).agent(agent)
                .actionType(ActionType.DELETE_FILE).status(RequestStatus.REJECTED).idempotencyKey("k2").build();
        when(actionRequestRepository.findAll()).thenReturn(List.of(executed, rejected));
        when(executionRecordRepository.findByActionRequestId(executed.getId()))
                .thenReturn(Optional.of(ExecutionRecord.builder().result("File read").build()));
        when(executionRecordRepository.findByActionRequestId(rejected.getId())).thenReturn(Optional.empty());

        List<ActionRequestResponse> responses = actionRequestService.getAllRequests();

        assertEquals(2, responses.size());
        assertEquals("File read", responses.get(0).getExecutionResult());
        assertNull(responses.get(1).getExecutionResult());
    }
}
