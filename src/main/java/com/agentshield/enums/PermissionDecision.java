package com.agentshield.enums;

/**
 * The permission decision for a given action type.
 *
 * ALLOW            — action is automatically permitted.
 * DENY             — action is automatically rejected.
 * REQUIRE_APPROVAL — action needs human approval before execution.
 */
public enum PermissionDecision {
    ALLOW,
    DENY,
    REQUIRE_APPROVAL
}
