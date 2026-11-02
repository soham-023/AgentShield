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
 * Response returned ONLY when an agent is first registered or when an API key is rotated.
 * This is the ONLY time the unhashed raw API key is ever returned to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentResponse {

    private UUID id;
    private String name;
    private String description;
    private AgentStatus status;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private String apiKey;
    private String warning;

    public static CreateAgentResponse fromEntity(Agent agent, String rawApiKey) {
        return CreateAgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .status(agent.getStatus())
                .ownerId(agent.getOwner() != null ? agent.getOwner().getId() : null)
                .createdAt(agent.getCreatedAt())
                .apiKey(rawApiKey)
                .warning("Store this API key safely. You will not be able to see it again.")
                .build();
    }
}
