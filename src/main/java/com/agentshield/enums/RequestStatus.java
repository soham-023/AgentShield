package com.agentshield.enums;

/**
 * Status of an action request as it moves through the pipeline.
 *
 * PENDING_APPROVAL — waiting for human approval.
 * APPROVED         — approved by a human, ready for execution.
 * REJECTED         — denied by policy or rejected by a human.
 * ALLOWED          — automatically allowed by policy, ready for execution.
 * EXECUTED         — action has been successfully executed (simulated).
 * FAILED           — action execution failed.
 * EXPIRED          — approval window expired before a decision was made.
 */
public enum RequestStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ALLOWED,
    EXECUTED,
    FAILED,
    EXPIRED
}
