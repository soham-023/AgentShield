package com.agentshield.repository;

import com.agentshield.entity.Agent;
import com.agentshield.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    /**
     * Find all agents owned by a specific user.
     */
    List<Agent> findByOwnerId(UUID ownerId);

    /**
     * Find a specific agent that belongs to a specific user.
     * Used to enforce ownership — users can only access their own agents.
     */
    Optional<Agent> findByIdAndOwnerId(UUID agentId, UUID ownerId);

    /**
     * Find an agent by the hash of its API key.
     * Used for agent authentication.
     */
    Optional<Agent> findByApiKeyHash(String apiKeyHash);

    /**
     * Count agents by status.
     */
    long countByStatus(AgentStatus status);

    /**
     * Count agents by owner.
     */
    long countByOwnerId(UUID ownerId);
}
