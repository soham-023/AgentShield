package com.agentshield.enums;

/**
 * Types of actions an AI agent can request.
 * These represent the "verbs" an agent might want to perform.
 *
 * In a real system, these would map to actual external service calls.
 * In AgentShield v1, they are simulated.
 */
public enum ActionType {
    READ_EMAIL,
    SEND_EMAIL,
    DELETE_FILE,
    READ_FILE,
    UPDATE_DATABASE,
    EXECUTE_REPORT
}
