package com.agentshield.controller;

import com.agentshield.dto.request.CreatePolicyRequest;
import com.agentshield.dto.response.PolicyResponse;
import com.agentshield.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/{agentId}/policies")
@RequiredArgsConstructor
@Tag(name = "Permission Policies", description = "Endpoints for configuring agent permission policies")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Set or update policy for an action", description = "Defines whether an action is ALLOW, DENY, or REQUIRE_APPROVAL for this agent")
    public ResponseEntity<PolicyResponse> createOrUpdatePolicy(
            @PathVariable UUID agentId,
            @Valid @RequestBody CreatePolicyRequest request) {
        PolicyResponse response = policyService.createOrUpdatePolicy(agentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all policies for an agent", description = "Retrieves all configured permission rules for this agent")
    public ResponseEntity<List<PolicyResponse>> getPolicies(@PathVariable UUID agentId) {
        return ResponseEntity.ok(policyService.getPoliciesByAgentId(agentId));
    }

    @DeleteMapping("/{policyId}")
    @Operation(summary = "Delete a policy", description = "Removes a policy rule (agent will fall back to Default Deny)")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable UUID agentId,
            @PathVariable UUID policyId) {
        policyService.deletePolicy(agentId, policyId);
        return ResponseEntity.noContent().build();
    }
}
