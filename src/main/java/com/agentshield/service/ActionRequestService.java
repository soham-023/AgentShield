package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.SubmitActionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.Agent;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.enums.*;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.BadRequestException;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.executor.SimulatedActionExecutor;
import com.agentshield.repository.ActionRequestRepository;
import com.agentshield.repository.AgentRepository;
import com.agentshield.repository.ExecutionRecordRepository;
import com.agentshield.security.ApiHeaders;
import com.agentshield.util.ApiKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles action requests submitted by agents: authenticates the agent by
 * API key, evaluates its policy, then executes, rejects, or parks the request
 * for human approval, recording each step in the audit log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActionRequestService {

    /** Audit result recorded when a policy allowed an action and it ran. */
    static final String RESULT_ALLOWED_AND_EXECUTED = "ALLOWED_AND_EXECUTED";
    /** Audit result recorded when a policy denied an action. */
    static final String RESULT_DENIED_BY_POLICY = "DENIED_BY_POLICY";
    /** Audit result recorded when an action was parked for human approval. */
    static final String RESULT_PENDING_HUMAN_APPROVAL = "PENDING_HUMAN_APPROVAL";

    private final ActionRequestRepository actionRequestRepository;
    private final AgentRepository agentRepository;
    private final PolicyService policyService;
    private final SimulatedActionExecutor actionExecutor;
    private final AuditService auditService;
    private final ExecutionRecordRepository executionRecordRepository;

    /** How long a REQUIRE_APPROVAL request waits for a human before it expires. */
    @Value("${agentshield.approval.expiration-hours:24}")
    private long approvalExpirationHours = 24;

    /**
     * Authenticates the agent, evaluates its policy for the requested action and
     * then executes it ({@code ALLOW}), rejects it ({@code DENY}) or parks it for a
     * human ({@code REQUIRE_APPROVAL}). A repeated idempotency key returns the
     * request already stored under it instead of creating a new one.
     *
     * @param rawApiKey the agent's raw API key from the {@code X-Agent-API-Key} header
     * @param request   the action the agent wants to take
     * @return the stored request, including the execution result when it ran
     * @throws AccessDeniedException if the key is missing or matches no agent
     * @throws BadRequestException   if the agent is disabled
     */
    @Transactional
    public ActionRequestResponse processActionRequest(String rawApiKey, SubmitActionRequest request) {
        // 1. Authenticate Agent by hashing the incoming API Key
        Agent agent = authenticateAgent(rawApiKey);

        // 2. Guard: verify agent is ACTIVE
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new BadRequestException("Agent is DISABLED and cannot request actions");
        }

        // 3. Idempotency Check: if request key was already processed, return existing record
        String idempotencyKey = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey().trim()
                : UUID.randomUUID().toString();

        Optional<ActionRequest> existing = actionRequestRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request detected. Returning existing record: {}", existing.get().getId());
            return toResponse(existing.get());
        }

        // 4. Policy Evaluation (Default Deny if no policy set)
        PermissionDecision decision = policyService.evaluatePermission(agent, request.getActionType());

        // 5. Build ActionRequest entity
        ActionRequest actionRequest = ActionRequest.builder()
                .agent(agent)
                .actionType(request.getActionType())
                .description(request.getDescription())
                .parameters(request.getParameters())
                .decision(decision)
                .idempotencyKey(idempotencyKey)
                .build();

        String executionResult = null;

        switch (decision) {
            case ALLOW -> {
                actionRequest.setStatus(RequestStatus.ALLOWED);
                actionRequest = actionRequestRepository.save(actionRequest);

                // Execute immediately
                ExecutionRecord record = actionExecutor.execute(actionRequest);
                executionResult = record.getResult();

                auditService.recordEvent(
                        agent.getId(),
                        agent.getId(),
                        actionRequest.getId(),
                        AuditEventType.ACTION_EXECUTED,
                        RESULT_ALLOWED_AND_EXECUTED,
                        Map.of("actionType", request.getActionType().name())
                );
            }
            case DENY -> {
                actionRequest.setStatus(RequestStatus.REJECTED);
                actionRequest = actionRequestRepository.save(actionRequest);

                auditService.recordEvent(
                        agent.getId(),
                        agent.getId(),
                        actionRequest.getId(),
                        AuditEventType.ACTION_DENIED,
                        RESULT_DENIED_BY_POLICY,
                        Map.of("actionType", request.getActionType().name())
                );
            }
            case REQUIRE_APPROVAL -> {
                actionRequest.setStatus(RequestStatus.PENDING_APPROVAL);
                actionRequest.setExpiresAt(LocalDateTime.now().plusHours(approvalExpirationHours));
                actionRequest = actionRequestRepository.save(actionRequest);

                auditService.recordEvent(
                        agent.getId(),
                        agent.getId(),
                        actionRequest.getId(),
                        AuditEventType.ACTION_PENDING_APPROVAL,
                        RESULT_PENDING_HUMAN_APPROVAL,
                        Map.of("actionType", request.getActionType().name())
                );
            }
        }

        return ActionRequestResponse.fromEntity(actionRequest, executionResult);
    }

    /** Returns every action request, each with its execution result if it ran. */
    @Transactional(readOnly = true)
    public List<ActionRequestResponse> getAllRequests() {
        return actionRequestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns one action request by id.
     *
     * @throws ResourceNotFoundException if no request has that id
     */
    @Transactional(readOnly = true)
    public ActionRequestResponse getRequestById(UUID id) {
        ActionRequest req = actionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionRequest", "id", id));
        return toResponse(req);
    }

    private Agent authenticateAgent(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new AccessDeniedException("Missing required '" + ApiHeaders.AGENT_API_KEY + "' header");
        }
        String keyHash = ApiKeyUtils.hashApiKey(rawApiKey.trim());
        return agentRepository.findByApiKeyHash(keyHash)
                .orElseThrow(() -> new AccessDeniedException("Invalid Agent API key"));
    }

    private ActionRequestResponse toResponse(ActionRequest req) {
        String result = executionRecordRepository.findByActionRequestId(req.getId())
                .map(ExecutionRecord::getResult)
                .orElse(null);
        return ActionRequestResponse.fromEntity(req, result);
    }
}
