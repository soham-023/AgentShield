package com.agentshield.dto.request;

import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
