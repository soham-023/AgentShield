package com.agentshield.entity;

import com.agentshield.enums.ActionType;
import com.agentshield.enums.PermissionDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a permission rule for a specific agent and action type.
 *
 * Example: Agent "CollegeAssistant" has policy:
 *   READ_EMAIL  → ALLOW
 *   SEND_EMAIL  → REQUIRE_APPROVAL
 *   DELETE_FILE → DENY
 *
 * The unique constraint on (agent_id, action_type) ensures each agent
 * has at most ONE policy per action type. Without this, we could have
 * conflicting rules like:
 *   SEND_EMAIL → ALLOW
 *   SEND_EMAIL → DENY
 */
@Entity
@Table(
    name = "permission_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_agent_action_type",
            columnNames = {"agent_id", "action_type"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PermissionDecision decision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
