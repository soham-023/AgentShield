package com.agentshield.repository;

import com.agentshield.entity.AuditLog;
import com.agentshield.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by actor (user or agent).
     */
    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);

    /**
     * Find audit logs by agent.
     */
    Page<AuditLog> findByAgentId(UUID agentId, Pageable pageable);

    /**
     * Find audit logs by event type.
     */
    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    /**
     * Find all audit logs, ordered by creation time descending.
     */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
