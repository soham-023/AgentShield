package com.agentshield.dto.response;

import com.agentshield.entity.ActionRequest;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequestResponse {

    private UUID id;
    private UUID agentId;
    private String agentName;
    private ActionType actionType;
    private String description;
    private Map<String, Object> parameters;
    private PermissionDecision decision;
    private RequestStatus status;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String executionResult;

    public static ActionRequestResponse fromEntity(ActionRequest request) {
        return fromEntity(request, null);
    }

    public static ActionRequestResponse fromEntity(ActionRequest request, String executionResult) {
        return ActionRequestResponse.builder()
                .id(request.getId())
                .agentId(request.getAgent().getId())
                .agentName(request.getAgent().getName())
                .actionType(request.getActionType())
                .description(request.getDescription())
                .parameters(request.getParameters())
                .decision(request.getDecision())
                .status(request.getStatus())
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(request.getCreatedAt())
                .expiresAt(request.getExpiresAt())
                .executionResult(executionResult)
                .build();
    }
}
