package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.CreatePolicyRequest;
import com.agentshield.dto.response.PolicyResponse;
import com.agentshield.entity.Agent;
import com.agentshield.entity.PermissionPolicy;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.AuditEventType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.repository.PermissionPolicyRepository;
import com.agentshield.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PermissionPolicyRepository policyRepository;
    private final AgentService agentService;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;

    /**
     * Creates or updates a permission policy for an agent.
     * Enforces the rule: at most one policy per (agent, actionType).
     */
    @Transactional
    public PolicyResponse createOrUpdatePolicy(UUID agentId, CreatePolicyRequest request) {
        Agent agent = agentService.getAgentAndVerifyOwnership(agentId);
        UUID currentUserId = securityUtils.getCurrentUser().getId();

        Optional<PermissionPolicy> existing = policyRepository
                .findByAgentIdAndActionType(agentId, request.getActionType());

        PermissionPolicy policy;
        AuditEventType eventType;

        if (existing.isPresent()) {
            policy = existing.get();
            policy.setDecision(request.getDecision());
            eventType = AuditEventType.POLICY_UPDATED;
        } else {
            policy = PermissionPolicy.builder()
                    .agent(agent)
                    .actionType(request.getActionType())
                    .decision(request.getDecision())
                    .build();
            eventType = AuditEventType.POLICY_CREATED;
        }

        PermissionPolicy saved = policyRepository.save(policy);

        auditService.recordEvent(
                currentUserId,
                agentId,
                null,
                eventType,
                saved.getDecision().name(),
                Map.of("actionType", saved.getActionType().name())
        );

        return PolicyResponse.fromEntity(saved);
    }

    /**
     * Retrieves all policies defined for a specific agent.
     */
    @Transactional(readOnly = true)
    public List<PolicyResponse> getPoliciesByAgentId(UUID agentId) {
        agentService.getAgentAndVerifyOwnership(agentId);
        return policyRepository.findByAgentId(agentId).stream()
                .map(PolicyResponse::fromEntity)
                .toList();
    }

    /**
     * Deletes a policy.
     */
    @Transactional
    public void deletePolicy(UUID agentId, UUID policyId) {
        agentService.getAgentAndVerifyOwnership(agentId);
        PermissionPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", "id", policyId));

        policyRepository.delete(policy);

        auditService.recordEvent(
                securityUtils.getCurrentUser().getId(),
                agentId,
                null,
                AuditEventType.POLICY_DELETED,
                "SUCCESS",
                Map.of("actionType", policy.getActionType().name())
        );
    }

    /**
     * EVALUATION ENGINE:
     * Evaluates permission for a given agent and action type.
     * RULE: If no policy is found, DENY by default!
     */
    @Transactional(readOnly = true)
    public PermissionDecision evaluatePermission(Agent agent, ActionType actionType) {
        return policyRepository.findByAgentIdAndActionType(agent.getId(), actionType)
                .map(PermissionPolicy::getDecision)
                .orElse(PermissionDecision.DENY); // Default Deny
    }
}
