package com.agentshield.dto.request;

import com.agentshield.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Map<String, Object> parameters;

    /**
     * Unique key from agent to prevent duplicate requests.
     * If omitted, the service will generate a UUID.
     */
    @Size(max = 255, message = "Idempotency key cannot exceed 255 characters")
    private String idempotencyKey;
}
