package com.agentshield.enums;

/**
 * Types of events recorded in the audit log.
 * Every significant action in the system creates an audit entry.
 */
public enum AuditEventType {
    USER_REGISTERED,
    USER_LOGIN,
    AGENT_CREATED,
    AGENT_UPDATED,
    AGENT_DISABLED,
    AGENT_ENABLED,
    AGENT_DELETED,
    API_KEY_ROTATED,
    POLICY_CREATED,
    POLICY_UPDATED,
    POLICY_DELETED,
    ACTION_REQUESTED,
    ACTION_ALLOWED,
    ACTION_DENIED,
    ACTION_PENDING_APPROVAL,
    ACTION_APPROVED,
    ACTION_REJECTED,
    ACTION_EXECUTED,
    ACTION_EXECUTION_FAILED,
    ACTION_EXPIRED
}
