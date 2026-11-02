package com.agentshield.repository;

import com.agentshield.entity.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, UUID> {

    /**
     * Find execution record for a specific action request.
     */
    Optional<ExecutionRecord> findByActionRequestId(UUID actionRequestId);

    /**
     * Check if an action has already been executed.
     * Prevents duplicate execution.
     */
    boolean existsByActionRequestId(UUID actionRequestId);
}
