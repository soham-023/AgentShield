package com.agentshield.dto.response;

import com.agentshield.entity.Agent;
import com.agentshield.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Standard agent response DTO.
 * Notice: We NEVER include apiKeyHash in any response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private UUID id;
    private String name;
    private String description;
    private AgentStatus status;
    private UUID ownerId;
    private String ownerEmail;
    private LocalDateTime createdAt;

    public static AgentResponse fromEntity(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .status(agent.getStatus())
                .ownerId(agent.getOwner() != null ? agent.getOwner().getId() : null)
                .ownerEmail(agent.getOwner() != null ? agent.getOwner().getEmail() : null)
                .createdAt(agent.getCreatedAt())
                .build();
    }
}
