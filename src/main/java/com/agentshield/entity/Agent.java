package com.agentshield.entity;

import com.agentshield.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a registered AI agent.
 *
 * An agent is a software identity (not a human). It authenticates via an API key
 * and submits action requests that are checked against its permission policies.
 *
 * Important: We store only the HASH of the API key. The raw key is shown once
 * at creation time and never stored or returned again. This is the same pattern
 * used by GitHub, Stripe, and other platforms for API key security.
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * The user who owns this agent.
     * @ManyToOne — many agents can belong to one user.
     * @JoinColumn — specifies the foreign key column name in the agents table.
     * FetchType.LAZY — don't load the owner until we explicitly access it.
     *   (This prevents the "N+1 query" problem when listing agents.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * SHA-256 hash of the agent's API key.
     * The raw API key is generated once and returned to the user.
     * We only store the hash for verification.
     */
    @Column(name = "api_key_hash", nullable = false, unique = true)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Permission policies attached to this agent.
     */
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PermissionPolicy> policies = new ArrayList<>();

    /**
     * Action requests submitted by this agent.
     */
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ActionRequest> actionRequests = new ArrayList<>();
}
