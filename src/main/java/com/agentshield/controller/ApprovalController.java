package com.agentshield.controller;

import com.agentshield.dto.request.ApprovalDecisionRequest;
import com.agentshield.dto.response.ActionRequestResponse;
import com.agentshield.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Approval Management", description = "Endpoints for human owners to review and approve/reject pending agent actions")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @Operation(summary = "List pending approvals", description = "Retrieves all action requests currently waiting for human approval")
    public ResponseEntity<List<ActionRequestResponse>> getPendingApprovals() {
        return ResponseEntity.ok(approvalService.getPendingApprovals());
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Approve an action request", description = "Authorizes the request and executes the simulated action")
    public ResponseEntity<ActionRequestResponse> approve(
            @PathVariable UUID requestId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(approvalService.approve(requestId, request));
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "Reject an action request", description = "Rejects the request and prevents execution")
    public ResponseEntity<ActionRequestResponse> reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return ResponseEntity.ok(approvalService.reject(requestId, request));
    }
}
