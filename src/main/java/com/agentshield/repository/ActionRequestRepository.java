package com.agentshield.repository;

import com.agentshield.entity.ActionRequest;
import com.agentshield.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionRequestRepository extends JpaRepository<ActionRequest, UUID> {

    /**
     * Find an existing request by its idempotency key.
     * Used to prevent duplicate action requests.
     */
    Optional<ActionRequest> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find all requests for a specific agent.
     */
    Page<ActionRequest> findByAgentId(UUID agentId, Pageable pageable);

    /**
     * Find all requests for agents owned by a specific user.
     */
    @Query("SELECT ar FROM ActionRequest ar WHERE ar.agent.owner.id = :ownerId")
    Page<ActionRequest> findByAgentOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    /**
     * Find all pending approval requests for agents owned by a specific user.
     */
    @Query("SELECT ar FROM ActionRequest ar WHERE ar.agent.owner.id = :ownerId AND ar.status = :status")
    List<ActionRequest> findByAgentOwnerIdAndStatus(
        @Param("ownerId") UUID ownerId,
        @Param("status") RequestStatus status
    );

    /**
     * Find expired pending requests (for background cleanup).
     */
    List<ActionRequest> findByStatusAndExpiresAtBefore(RequestStatus status, LocalDateTime expiresBefore);

    /**
     * Count requests by status.
     */
    long countByStatus(RequestStatus status);

    /**
     * Count requests by action type (for dashboard statistics).
     */
    @Query("SELECT ar.actionType, COUNT(ar) FROM ActionRequest ar GROUP BY ar.actionType")
    List<Object[]> countByActionType();
}
