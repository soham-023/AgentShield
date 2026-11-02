package com.agentshield.service;

import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.UpdateAgentRequest;
import com.agentshield.dto.response.AgentResponse;
import com.agentshield.dto.response.CreateAgentResponse;
import com.agentshield.entity.Agent;
import com.agentshield.entity.User;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.repository.AgentRepository;
import com.agentshield.security.SecurityUtils;
import com.agentshield.util.ApiKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;
    private final SecurityUtils securityUtils;

    /**
     * Registers a new AI agent for the current user.
     * Generates a raw API key, hashes it with SHA-256, stores only the hash,
     * and returns the raw key in the response once.
     */
    @Transactional
    public CreateAgentResponse createAgent(CreateAgentRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // 1. Generate secure raw API key (e.g. as_live_...)
        String rawApiKey = ApiKeyUtils.generateApiKey();

        // 2. Hash the key with SHA-256 for database storage
        String apiKeyHash = ApiKeyUtils.hashApiKey(rawApiKey);

        // 3. Persist agent entity
        Agent agent = Agent.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .owner(currentUser)
                .apiKeyHash(apiKeyHash)
                .status(AgentStatus.ACTIVE)
                .build();

        Agent savedAgent = agentRepository.save(agent);
        log.info("Agent created successfully with ID: {} by User: {}", savedAgent.getId(), currentUser.getEmail());

        // 4. Return DTO with raw key (shown only once)
        return CreateAgentResponse.fromEntity(savedAgent, rawApiKey);
    }

    /**
     * Retrieves all agents owned by the current user (or all agents if ADMIN).
     */
    @Transactional(readOnly = true)
    public List<AgentResponse> getAllAgents() {
        User currentUser = securityUtils.getCurrentUser();

        List<Agent> agents;
        if (currentUser.getRole() == UserRole.ADMIN) {
            agents = agentRepository.findAll();
        } else {
            agents = agentRepository.findByOwnerId(currentUser.getId());
        }

        return agents.stream()
                .map(AgentResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single agent by ID after verifying ownership.
     */
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(UUID agentId) {
        Agent agent = getAgentAndVerifyOwnership(agentId);
        return AgentResponse.fromEntity(agent);
    }

    /**
     * Updates an agent's name, description, or status.
     */
    @Transactional
    public AgentResponse updateAgent(UUID agentId, UpdateAgentRequest request) {
        Agent agent = getAgentAndVerifyOwnership(agentId);

        agent.setName(request.getName().trim());
        agent.setDescription(request.getDescription());
        agent.setStatus(request.getStatus());

        Agent updatedAgent = agentRepository.save(agent);
        log.info("Agent updated successfully with ID: {}", updatedAgent.getId());

        return AgentResponse.fromEntity(updatedAgent);
    }

    /**
     * Deletes an agent.
     */
    @Transactional
    public void deleteAgent(UUID agentId) {
        Agent agent = getAgentAndVerifyOwnership(agentId);
        agentRepository.delete(agent);
        log.info("Agent deleted with ID: {}", agentId);
    }

    /**
     * Rotates an agent's API key.
     * Generates a new raw key, replaces the hash in the DB,
     * and returns the new raw key once.
     */
    @Transactional
    public CreateAgentResponse rotateApiKey(UUID agentId) {
        Agent agent = getAgentAndVerifyOwnership(agentId);

        String newRawApiKey = ApiKeyUtils.generateApiKey();
        String newApiKeyHash = ApiKeyUtils.hashApiKey(newRawApiKey);

        agent.setApiKeyHash(newApiKeyHash);
        Agent updatedAgent = agentRepository.save(agent);
        log.info("API key rotated for agent ID: {}", agentId);

        return CreateAgentResponse.fromEntity(updatedAgent, newRawApiKey);
    }

    /**
     * Helper to retrieve an agent by ID and verify the current user owns it
     * (or is an ADMIN).
     */
    public Agent getAgentAndVerifyOwnership(UUID agentId) {
        User currentUser = securityUtils.getCurrentUser();

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        boolean isOwner = agent.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to access or modify this agent");
        }

        return agent;
    }
}
