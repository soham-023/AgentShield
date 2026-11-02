package com.agentshield.repository;

import com.agentshield.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    /**
     * Find the approval record for a specific action request.
     */
    Optional<Approval> findByActionRequestId(UUID actionRequestId);

    /**
     * Check if an approval already exists for this request.
     * Prevents double-approval.
     */
    boolean existsByActionRequestId(UUID actionRequestId);
}
