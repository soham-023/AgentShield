package com.agentshield.entity;

import com.agentshield.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records the result of a simulated action execution.
 *
 * In AgentShield v1, no real actions are performed.
 * This entity records WHAT WOULD HAVE HAPPENED — e.g.,
 * "Simulated sending email to professor@example.com".
 */
@Entity
@Table(
    name = "execution_records",
    indexes = {
        @Index(name = "idx_execution_request", columnList = "action_request_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_request_id", nullable = false, unique = true)
    private ActionRequest actionRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 20)
    private ExecutionStatus executionStatus;

    @Column(columnDefinition = "TEXT")
    private String result;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;
}
