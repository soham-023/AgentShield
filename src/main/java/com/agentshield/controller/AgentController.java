package com.agentshield.controller;

import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.UpdateAgentRequest;
import com.agentshield.dto.response.AgentResponse;
import com.agentshield.dto.response.CreateAgentResponse;
import com.agentshield.service.AgentService;
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
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Management", description = "Endpoints for registering, viewing, modifying, and rotating keys for AI agents")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @Operation(summary = "Register a new AI agent", description = "Creates a new agent and returns the plain API key once. The raw key cannot be retrieved again.")
    public ResponseEntity<CreateAgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        CreateAgentResponse response = agentService.createAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all agents", description = "Returns all agents owned by the calling user, or all system agents for ADMIN")
    public ResponseEntity<List<AgentResponse>> getAllAgents() {
        List<AgentResponse> agents = agentService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{agentId}")
    @Operation(summary = "Get agent by ID", description = "Retrieves details of a specific agent by its UUID")
    public ResponseEntity<AgentResponse> getAgentById(@PathVariable UUID agentId) {
        AgentResponse response = agentService.getAgentById(agentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{agentId}")
    @Operation(summary = "Update an agent", description = "Updates an agent's name, description, or active/disabled status")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable UUID agentId,
            @Valid @RequestBody UpdateAgentRequest request) {
        AgentResponse response = agentService.updateAgent(agentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{agentId}")
    @Operation(summary = "Delete an agent", description = "Deletes an agent and cascades to its policies and requests")
    public ResponseEntity<Void> deleteAgent(@PathVariable UUID agentId) {
        agentService.deleteAgent(agentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{agentId}/rotate-api-key")
    @Operation(summary = "Rotate agent API key", description = "Invalidates the previous API key and generates a new one. The new key is returned once.")
    public ResponseEntity<CreateAgentResponse> rotateApiKey(@PathVariable UUID agentId) {
        CreateAgentResponse response = agentService.rotateApiKey(agentId);
        return ResponseEntity.ok(response);
    }
}
