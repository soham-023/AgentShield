package com.agentshield.repository;

import com.agentshield.entity.PermissionPolicy;
import com.agentshield.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionPolicyRepository extends JpaRepository<PermissionPolicy, UUID> {

    /**
     * Find all policies for a given agent.
     */
    List<PermissionPolicy> findByAgentId(UUID agentId);

    /**
     * Find the policy for a specific agent and action type.
     * Returns Optional because there might not be a policy (→ default DENY).
     */
    Optional<PermissionPolicy> findByAgentIdAndActionType(UUID agentId, ActionType actionType);

    /**
     * Check if a policy already exists for this agent + action type combination.
     */
    boolean existsByAgentIdAndActionType(UUID agentId, ActionType actionType);
}
