package com.agentshield.dto.response;

import com.agentshield.entity.PermissionPolicy;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private UUID id;
    private UUID agentId;
    private ActionType actionType;
    private PermissionDecision decision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PolicyResponse fromEntity(PermissionPolicy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .agentId(policy.getAgent().getId())
                .actionType(policy.getActionType())
                .decision(policy.getDecision())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
