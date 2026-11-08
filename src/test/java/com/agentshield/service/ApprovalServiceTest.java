package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.ApprovalDecisionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.Agent;
import com.agentshield.entity.Approval;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.entity.User;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.ApprovalDecision;
import com.agentshield.enums.AuditEventType;
import com.agentshield.enums.ExecutionStatus;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.RequestStatus;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.BadRequestException;
import com.agentshield.executor.SimulatedActionExecutor;
import com.agentshield.repository.ActionRequestRepository;
import com.agentshield.repository.ApprovalRepository;
import com.agentshield.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ActionRequestRepository actionRequestRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private SimulatedActionExecutor actionExecutor;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ApprovalService approvalService;

    private User owner;
    private User otherUser;
    private ActionRequest pendingRequest;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).name("Owner")
                .email("owner@agentshield.com").role(UserRole.USER).build();
        otherUser = User.builder().id(UUID.randomUUID()).name("Other")
                .email("other@agentshield.com").role(UserRole.USER).build();

        Agent agent = Agent.builder().id(UUID.randomUUID()).name("Mail Agent")
                .owner(owner).apiKeyHash("hash").status(AgentStatus.ACTIVE).build();

        requestId = UUID.randomUUID();
        pendingRequest = ActionRequest.builder()
                .id(requestId)
                .agent(agent)
                .actionType(ActionType.SEND_EMAIL)
                .decision(PermissionDecision.REQUIRE_APPROVAL)
                .status(RequestStatus.PENDING_APPROVAL)
                .idempotencyKey("key-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("Owner approval executes the action and records an APPROVED decision")
    void ownerCanApprove() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(approvalRepository.existsByActionRequestId(requestId)).thenReturn(false);
        when(actionExecutor.execute(pendingRequest)).thenReturn(ExecutionRecord.builder()
                .executionStatus(ExecutionStatus.SUCCESS).result("SIMULATED: sent").build());

        ActionRequestResponse response = approvalService.approve(requestId, new ApprovalDecisionRequest("Looks good"));

        assertEquals("SIMULATED: sent", response.getExecutionResult());
        ArgumentCaptor<Approval> captor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository).save(captor.capture());
        assertEquals(ApprovalDecision.APPROVED, captor.getValue().getDecision());
        assertEquals("Looks good", captor.getValue().getComments());
        verify(auditService).recordEvent(eq(owner.getId()), any(), eq(requestId),
                eq(AuditEventType.ACTION_APPROVED), anyString(), anyMap());
    }

    @Test
    @DisplayName("Rejection marks the request REJECTED and never executes it")
    void ownerCanReject() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(approvalRepository.existsByActionRequestId(requestId)).thenReturn(false);

        ActionRequestResponse response = approvalService.reject(requestId, null);

        assertEquals(RequestStatus.REJECTED, response.getStatus());
        verify(actionExecutor, never()).execute(any());
        verify(auditService).recordEvent(eq(owner.getId()), any(), eq(requestId),
                eq(AuditEventType.ACTION_REJECTED), anyString(), anyMap());
    }

    @Test
    @DisplayName("A user who does not own the agent cannot approve")
    void nonOwnerCannotApprove() {
        when(securityUtils.getCurrentUser()).thenReturn(otherUser);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        assertThrows(AccessDeniedException.class, () -> approvalService.approve(requestId, null));
        verify(actionExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("A request that is not pending cannot be approved")
    void nonPendingRequestRejected() {
        pendingRequest.setStatus(RequestStatus.ALLOWED);
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        assertThrows(BadRequestException.class, () -> approvalService.approve(requestId, null));
    }

    @Test
    @DisplayName("An expired request is marked EXPIRED and cannot be approved")
    void expiredRequestIsMarkedExpired() {
        pendingRequest.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        assertThrows(BadRequestException.class, () -> approvalService.approve(requestId, null));
        assertEquals(RequestStatus.EXPIRED, pendingRequest.getStatus());
        verify(actionRequestRepository).save(pendingRequest);
        verify(actionExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("A request that was already decided cannot be decided again")
    void doubleDecisionRejected() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(actionRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(approvalRepository.existsByActionRequestId(requestId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> approvalService.reject(requestId, null));
        verify(approvalRepository, never()).save(any());
    }
}
