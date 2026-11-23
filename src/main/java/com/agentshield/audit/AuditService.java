package com.agentshield.audit;

import com.agentshield.entity.AuditLog;
import com.agentshield.enums.AuditEventType;
import com.agentshield.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for creating immutable audit log entries.
 * Logs are append-only and never updated or deleted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Appends one entry to the audit log.
     *
     * @param actorId         who caused the event: a user id, or the agent's own id for agent actions
     * @param agentId         the agent the event concerns
     * @param actionRequestId the related action request, or {@code null} if there is none
     * @param eventType       what happened
     * @param result          short machine-readable outcome, e.g. {@code DENIED_BY_POLICY}
     * @param metadata        extra context stored as JSON, or {@code null}
     * @return the saved entry
     */
    @Transactional
    public AuditLog recordEvent(
            UUID actorId,
            UUID agentId,
            UUID actionRequestId,
            AuditEventType eventType,
            String result,
            Map<String, Object> metadata) {
        AuditLog logEntry = AuditLog.builder()
                .actorId(actorId)
                .agentId(agentId)
                .actionRequestId(actionRequestId)
                .eventType(eventType)
                .result(result)
                .metadata(metadata)
                .build();

        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("AUDIT [{}] Actor: {} | Agent: {} | Result: {}", eventType, actorId, agentId, result);
        return saved;
    }
}
