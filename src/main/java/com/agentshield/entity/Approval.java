package com.agentshield.entity;

import com.agentshield.enums.ApprovalDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a human approval or rejection of an action request.
 *
 * Only requests with REQUIRE_APPROVAL decision generate approval records.
 * The approver must be the agent's owner or an admin.
 */
@Entity
@Table(
    name = "approvals",
    indexes = {
        @Index(name = "idx_approval_request", columnList = "action_request_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_request_id", nullable = false, unique = true)
    private ActionRequest actionRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(length = 500)
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
