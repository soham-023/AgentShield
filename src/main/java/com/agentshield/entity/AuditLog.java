package com.agentshield.entity;

import com.agentshield.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit log entry.
 *
 * Every significant event in the system is recorded here.
 * Audit logs are append-only — they should NEVER be updated or deleted.
 *
 * This is critical for:
 *   - Security compliance
 *   - Debugging production issues
 *   - Understanding what happened and when
 *
 * IMPORTANT: Never store passwords, raw API keys, or JWT tokens in metadata.
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_agent", columnList = "agent_id"),
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user or agent that performed the action.
     * This is a UUID stored as a string so it can reference either
     * a User ID or an Agent ID.
     */
    @Column(name = "actor_id")
    private UUID actorId;

    /**
     * The agent involved in the event (if applicable).
     */
    @Column(name = "agent_id")
    private UUID agentId;

    /**
     * The action request involved in the event (if applicable).
     */
    @Column(name = "action_request_id")
    private UUID actionRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(length = 50)
    private String result;

    /**
     * Additional context stored as JSON.
     * Examples: agent name, action type, IP address (but NEVER passwords or tokens).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
