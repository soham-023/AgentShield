package com.agentshield.entity;

import com.agentshield.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a human user of the AgentShield platform.
 *
 * Key JPA annotations:
 *   @Entity     — tells Hibernate this class maps to a database table.
 *   @Table      — specifies the table name (defaults to class name if omitted).
 *   @Id         — marks the primary key field.
 *   @Column     — customizes column properties (nullable, unique, length).
 *   @Enumerated — tells Hibernate how to store enum values (STRING = store "ADMIN", not 0).
 *   @OneToMany  — defines a one-to-many relationship (one user owns many agents).
 *
 * Lombok annotations:
 *   @Getter/@Setter  — generates getters and setters for all fields.
 *   @NoArgsConstructor — generates a no-argument constructor (required by JPA).
 *   @AllArgsConstructor — generates a constructor with all fields.
 *   @Builder           — generates a builder pattern for creating instances.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * One user can own many agents.
     * mappedBy = "owner" tells JPA that the Agent entity has a field called "owner"
     * that holds the foreign key. We manage the relationship from the Agent side.
     * cascade = ALL means if we delete a user, their agents are deleted too.
     * orphanRemoval = true cleans up agents removed from this list.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Agent> agents = new ArrayList<>();
}
