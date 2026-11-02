package com.agentshield.dto.request;

import com.agentshield.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitActionRequest {

    @NotNull(message = "Action type is required (e.g. SEND_EMAIL, READ_FILE)")
    private ActionType actionType;

    private String description;

    private Map<String, Object> parameters;

    /**
     * Unique key from agent to prevent duplicate requests.
     * If omitted, the service will generate a UUID.
     */
    private String idempotencyKey;
}
