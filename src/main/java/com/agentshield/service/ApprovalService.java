package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.ApprovalDecisionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.Approval;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.entity.User;
import com.agentshield.enums.ApprovalDecision;
import com.agentshield.enums.AuditEventType;
import com.agentshield.enums.RequestStatus;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.BadRequestException;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.executor.SimulatedActionExecutor;
import com.agentshield.repository.ActionRequestRepository;
import com.agentshield.repository.ApprovalRepository;
import com.agentshield.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ActionRequestRepository actionRequestRepository;
    private final ApprovalRepository approvalRepository;
    private final SimulatedActionExecutor actionExecutor;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    /**
     * Lists all action requests awaiting human approval.
     */
    @Transactional(readOnly = true)
    public List<ActionRequestResponse> getPendingApprovals() {
        User currentUser = securityUtils.getCurrentUser();

        List<ActionRequest> pendingList;
        if (currentUser.getRole() == UserRole.ADMIN) {
            pendingList = actionRequestRepository.findAll().stream()
                    .filter(ar -> ar.getStatus() == RequestStatus.PENDING_APPROVAL)
                    .toList();
        } else {
            pendingList = actionRequestRepository.findByAgentOwnerIdAndStatus(currentUser.getId(), RequestStatus.PENDING_APPROVAL);
        }

        return pendingList.stream()
                .map(ActionRequestResponse::fromEntity)
                .toList();
    }

    /**
     * Approves a pending action request and executes the simulated action.
     */
    @Transactional
    public ActionRequestResponse approve(UUID requestId, ApprovalDecisionRequest decisionRequest) {
        ActionRequest request = getAndValidatePendingRequest(requestId);
        User approver = securityUtils.getCurrentUser();

        // 1. Mark request as APPROVED
        request.setStatus(RequestStatus.APPROVED);

        // 2. Execute simulated action
        ExecutionRecord execution = actionExecutor.execute(request);

        // 3. Persist approval decision record
        Approval approval = Approval.builder()
                .actionRequest(request)
                .approver(approver)
                .decision(ApprovalDecision.APPROVED)
                .comments(decisionRequest != null ? decisionRequest.getComments() : null)
                .build();
        approvalRepository.save(approval);

        // 4. Record audit events
        auditService.recordEvent(
                approver.getId(),
                request.getAgent().getId(),
                request.getId(),
                AuditEventType.ACTION_APPROVED,
                "APPROVED_BY_HUMAN",
                Map.of("approver", approver.getEmail())
        );

        return ActionRequestResponse.fromEntity(request, execution.getResult());
    }

    /**
     * Rejects a pending action request.
     */
    @Transactional
    public ActionRequestResponse reject(UUID requestId, ApprovalDecisionRequest decisionRequest) {
        ActionRequest request = getAndValidatePendingRequest(requestId);
        User approver = securityUtils.getCurrentUser();

        // 1. Mark request as REJECTED
        request.setStatus(RequestStatus.REJECTED);
        actionRequestRepository.save(request);

        // 2. Persist rejection record
        Approval approval = Approval.builder()
                .actionRequest(request)
                .approver(approver)
                .decision(ApprovalDecision.REJECTED)
                .comments(decisionRequest != null ? decisionRequest.getComments() : null)
                .build();
        approvalRepository.save(approval);

        // 3. Record audit event
        auditService.recordEvent(
                approver.getId(),
                request.getAgent().getId(),
                request.getId(),
                AuditEventType.ACTION_REJECTED,
                "REJECTED_BY_HUMAN",
                Map.of("approver", approver.getEmail())
        );

        return ActionRequestResponse.fromEntity(request);
    }

    private ActionRequest getAndValidatePendingRequest(UUID requestId) {
        User currentUser = securityUtils.getCurrentUser();

        ActionRequest request = actionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ActionRequest", "id", requestId));

        // Ownership rule: only agent owner or ADMIN can approve
        boolean isOwner = request.getAgent().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to approve/reject this request");
        }

        // Must be in PENDING_APPROVAL status
        if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Request is not pending approval. Current status: " + request.getStatus());
        }

        // Check if expired
        if (request.getExpiresAt() != null && LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(RequestStatus.EXPIRED);
            actionRequestRepository.save(request);
            throw new BadRequestException("Action request has expired and can no longer be approved");
        }

        // Guard against double approval
        if (approvalRepository.existsByActionRequestId(requestId)) {
            throw new BadRequestException("Request has already been decided");
        }

        return request;
    }
}
