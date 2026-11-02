package com.agentshield.executor;

import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.ExecutionStatus;
import com.agentshield.enums.RequestStatus;
import com.agentshield.exception.BadRequestException;
import com.agentshield.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Executes simulated actions when permissions allow it or after approval.
 * Safe simulation: no real emails sent, files deleted, or production DB modified.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulatedActionExecutor {

    private final ExecutionRecordRepository executionRecordRepository;

    public ExecutionRecord execute(ActionRequest request) {
        // Guard: request must be in ALLOWED or APPROVED status
        if (request.getStatus() != RequestStatus.ALLOWED && request.getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException("Cannot execute request in status: " + request.getStatus());
        }

        // Guard: prevent repeated execution
        if (executionRecordRepository.existsByActionRequestId(request.getId())) {
            throw new BadRequestException("Action request has already been executed!");
        }

        String result = simulateAction(request.getActionType(), request.getParameters());

        ExecutionRecord record = ExecutionRecord.builder()
                .actionRequest(request)
                .executionStatus(ExecutionStatus.SUCCESS)
                .result(result)
                .executedAt(LocalDateTime.now())
                .build();

        ExecutionRecord saved = executionRecordRepository.save(record);
        request.setStatus(RequestStatus.EXECUTED);
        log.info("Successfully executed simulated action for request {}: {}", request.getId(), result);
        return saved;
    }

    private String simulateAction(ActionType type, Map<String, Object> params) {
        String p = params != null ? params.toString() : "{}";
        return switch (type) {
            case SEND_EMAIL -> "SIMULATED: Email successfully drafted and sent with params: " + p;
            case READ_EMAIL -> "SIMULATED: Read 3 unread emails from inbox. [Sample Subject: Project Update]";
            case DELETE_FILE -> "SIMULATED: File safely deleted: " + p;
            case READ_FILE -> "SIMULATED: File content loaded (4096 bytes read)";
            case UPDATE_DATABASE -> "SIMULATED: Database updated 1 row affected: " + p;
            case EXECUTE_REPORT -> "SIMULATED: Report generated. Total rows: 150, format: PDF";
        };
    }
}
