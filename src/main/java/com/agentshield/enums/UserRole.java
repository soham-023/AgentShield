package com.agentshield.enums;

/**
 * Roles that a user can have in the system.
 *
 * ADMIN — full access to all agents, policies, approvals, and audit logs.
 * USER  — can manage their own agents and approve their agents' actions.
 */
public enum UserRole {
    ADMIN,
    USER
}
