package com.agentshield.dto.request;

import com.agentshield.enums.AgentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code PUT /api/v1/agents/{agentId}}. Replaces the agent's name,
 * description and status; set status to {@code DISABLED} to block the agent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentRequest {

    @NotBlank(message = "Agent name is required")
    @Size(min = 2, max = 100, message = "Agent name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Agent status is required (ACTIVE or DISABLED)")
    private AgentStatus status;
}
