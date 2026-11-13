package com.agentshield.controller;

import com.agentshield.dto.request.SubmitActionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.security.ApiHeaders;
import com.agentshield.service.ActionRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent-actions")
@RequiredArgsConstructor
@Tag(name = "Action Requests", description = "Endpoints for AI agents to submit action requests")
public class ActionRequestController {

    private final ActionRequestService actionRequestService;

    @PostMapping
    @Operation(summary = "Submit an action request", description = "AI Agent presents its API key and requests an action (e.g., SEND_EMAIL)")
    public ResponseEntity<ActionRequestResponse> submitAction(
            @RequestHeader(value = ApiHeaders.AGENT_API_KEY, required = false) String apiKey,
            @Valid @RequestBody SubmitActionRequest request) {
        ActionRequestResponse response = actionRequestService.processActionRequest(apiKey, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all action requests", description = "View history of all submitted action requests")
    public ResponseEntity<List<ActionRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(actionRequestService.getAllRequests());
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get action request by ID", description = "View details and status of an action request")
    public ResponseEntity<ActionRequestResponse> getRequestById(@PathVariable UUID requestId) {
        return ResponseEntity.ok(actionRequestService.getRequestById(requestId));
    }
}
