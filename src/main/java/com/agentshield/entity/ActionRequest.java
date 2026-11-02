package com.agentshield.entity;

import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import com.agentshield.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an action request submitted by an AI agent.
 *
 * The lifecycle of a request:
 *   1. Agent submits request → status = ALLOWED / REJECTED / PENDING_APPROVAL
 *   2. If PENDING_APPROVAL → human approves/rejects → APPROVED / REJECTED
 *   3. If ALLOWED or APPROVED → agent calls execute → EXECUTED / FAILED
 *
 * The idempotency_key prevents duplicate requests. If an agent submits the
 * same request twice (e.g., due to a network retry), we return the existing
 * request instead of creating a duplicate.
 */
@Entity
@Table(
    name = "action_requests",
    indexes = {
        @Index(name = "idx_action_request_agent", columnList = "agent_id"),
        @Index(name = "idx_action_request_status", columnList = "status"),
        @Index(name = "idx_action_request_idempotency", columnList = "idempotency_key", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Column(length = 500)
    private String description;

    /**
     * Action parameters stored as JSON.
     * @JdbcTypeCode(SqlTypes.JSON) tells Hibernate to use PostgreSQL's JSONB type.
     * For H2 (tests), it falls back to a text column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters")
    private Map<String, Object> parameters;

    /**
     * The permission decision that was made for this request.
     * This records WHAT the policy said (ALLOW, DENY, REQUIRE_APPROVAL).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PermissionDecision decision;

    /**
     * The current status of the request in the lifecycle.
     * This tracks WHERE the request is (PENDING_APPROVAL, APPROVED, EXECUTED, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    /**
     * Client-provided idempotency key to prevent duplicate requests.
     * Must be unique across all requests.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * When a PENDING_APPROVAL request expires.
     * After this time, the request can no longer be approved.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
