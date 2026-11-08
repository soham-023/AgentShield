package com.agentshield.service;

import com.agentshield.audit.AuditService;
import com.agentshield.dto.request.CreatePolicyRequest;
import com.agentshield.dto.response.PolicyResponse;
import com.agentshield.entity.Agent;
import com.agentshield.entity.PermissionPolicy;
import com.agentshield.entity.User;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.AuditEventType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.repository.PermissionPolicyRepository;
import com.agentshield.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PermissionPolicyRepository policyRepository;

    @Mock
    private AgentService agentService;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private PolicyService policyService;

    private User owner;
    private Agent agent;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .name("Owner User")
                .email("owner@agentshield.com")
                .role(UserRole.USER)
                .build();

        agentId = UUID.randomUUID();
        agent = Agent.builder()
                .id(agentId)
                .name("Policy Agent")
                .owner(owner)
                .apiKeyHash("hash")
                .status(AgentStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Creates a new policy when none exists for the action type")
    void createsNewPolicy() {
        when(agentService.getAgentAndVerifyOwnership(agentId)).thenReturn(agent);
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(policyRepository.findByAgentIdAndActionType(agentId, ActionType.SEND_EMAIL))
                .thenReturn(Optional.empty());
        when(policyRepository.save(any(PermissionPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePolicyRequest request = new CreatePolicyRequest(ActionType.SEND_EMAIL, PermissionDecision.REQUIRE_APPROVAL);
        PolicyResponse response = policyService.createOrUpdatePolicy(agentId, request);

        assertEquals(agentId, response.getAgentId());
        assertEquals(ActionType.SEND_EMAIL, response.getActionType());
        assertEquals(PermissionDecision.REQUIRE_APPROVAL, response.getDecision());
        verify(auditService).recordEvent(eq(owner.getId()), eq(agentId), isNull(),
                eq(AuditEventType.POLICY_CREATED), eq("REQUIRE_APPROVAL"), anyMap());
    }

    @Test
    @DisplayName("Updates the existing policy instead of creating a duplicate")
    void updatesExistingPolicy() {
        PermissionPolicy existing = PermissionPolicy.builder()
                .id(UUID.randomUUID())
                .agent(agent)
                .actionType(ActionType.DELETE_FILE)
                .decision(PermissionDecision.ALLOW)
                .build();

        when(agentService.getAgentAndVerifyOwnership(agentId)).thenReturn(agent);
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(policyRepository.findByAgentIdAndActionType(agentId, ActionType.DELETE_FILE))
                .thenReturn(Optional.of(existing));
        when(policyRepository.save(existing)).thenReturn(existing);

        CreatePolicyRequest request = new CreatePolicyRequest(ActionType.DELETE_FILE, PermissionDecision.DENY);
        PolicyResponse response = policyService.createOrUpdatePolicy(agentId, request);

        assertEquals(existing.getId(), response.getId());
        assertEquals(PermissionDecision.DENY, response.getDecision());
        verify(policyRepository, times(1)).save(existing);
        verify(auditService).recordEvent(eq(owner.getId()), eq(agentId), isNull(),
                eq(AuditEventType.POLICY_UPDATED), eq("DENY"), anyMap());
    }

    @Test
    @DisplayName("Lists all policies for an agent")
    void listsPolicies() {
        PermissionPolicy p1 = PermissionPolicy.builder().id(UUID.randomUUID()).agent(agent)
                .actionType(ActionType.READ_EMAIL).decision(PermissionDecision.ALLOW).build();
        PermissionPolicy p2 = PermissionPolicy.builder().id(UUID.randomUUID()).agent(agent)
                .actionType(ActionType.SEND_EMAIL).decision(PermissionDecision.REQUIRE_APPROVAL).build();

        when(agentService.getAgentAndVerifyOwnership(agentId)).thenReturn(agent);
        when(policyRepository.findByAgentId(agentId)).thenReturn(List.of(p1, p2));

        List<PolicyResponse> responses = policyService.getPoliciesByAgentId(agentId);

        assertEquals(2, responses.size());
        assertEquals(ActionType.READ_EMAIL, responses.get(0).getActionType());
    }

    @Test
    @DisplayName("Deleting an unknown policy throws ResourceNotFoundException")
    void deleteUnknownPolicyThrows() {
        UUID policyId = UUID.randomUUID();
        when(agentService.getAgentAndVerifyOwnership(agentId)).thenReturn(agent);
        when(policyRepository.findById(policyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> policyService.deletePolicy(agentId, policyId));
        verify(policyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Evaluation returns the configured decision when a policy exists")
    void evaluateReturnsConfiguredDecision() {
        PermissionPolicy policy = PermissionPolicy.builder().agent(agent)
                .actionType(ActionType.READ_FILE).decision(PermissionDecision.ALLOW).build();
        when(policyRepository.findByAgentIdAndActionType(agentId, ActionType.READ_FILE))
                .thenReturn(Optional.of(policy));

        assertEquals(PermissionDecision.ALLOW, policyService.evaluatePermission(agent, ActionType.READ_FILE));
    }

    @Test
    @DisplayName("Evaluation denies by default when no policy exists")
    void evaluateDefaultsToDeny() {
        when(policyRepository.findByAgentIdAndActionType(agentId, ActionType.UPDATE_DATABASE))
                .thenReturn(Optional.empty());

        assertEquals(PermissionDecision.DENY, policyService.evaluatePermission(agent, ActionType.UPDATE_DATABASE));
    }
}
