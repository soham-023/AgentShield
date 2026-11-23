package com.agentshield.dto.request;

import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/v1/agents/{agentId}/policies}: the decision to apply
 * to one action type for this agent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePolicyRequest {

    @NotNull(message = "Action type is required (e.g. SEND_EMAIL, DELETE_FILE)")
    private ActionType actionType;

    @NotNull(message = "Permission decision is required (ALLOW, DENY, REQUIRE_APPROVAL)")
    private PermissionDecision decision;
}
