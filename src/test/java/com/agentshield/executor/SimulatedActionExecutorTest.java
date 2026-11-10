package com.agentshield.executor;

import com.agentshield.entity.ActionRequest;
import com.agentshield.entity.ExecutionRecord;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.ExecutionStatus;
import com.agentshield.enums.RequestStatus;
import com.agentshield.exception.BadRequestException;
import com.agentshield.repository.ExecutionRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulatedActionExecutorTest {

    @Mock
    private ExecutionRecordRepository executionRecordRepository;

    @InjectMocks
    private SimulatedActionExecutor executor;

    private ActionRequest request(RequestStatus status, ActionType type, Map<String, Object> params) {
        return ActionRequest.builder()
                .id(UUID.randomUUID())
                .status(status)
                .actionType(type)
                .parameters(params)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"ALLOWED", "APPROVED"})
    @DisplayName("executes ALLOWED and APPROVED requests and marks them EXECUTED")
    void executesRunnableRequests(RequestStatus status) {
        ActionRequest req = request(status, ActionType.SEND_EMAIL, Map.of("to", "a@example.com"));
        when(executionRecordRepository.existsByActionRequestId(req.getId())).thenReturn(false);
        when(executionRecordRepository.save(any(ExecutionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutionRecord record = executor.execute(req);

        assertEquals(ExecutionStatus.SUCCESS, record.getExecutionStatus());
        assertSame(req, record.getActionRequest());
        assertNotNull(record.getExecutedAt());
        assertTrue(record.getResult().startsWith("SIMULATED:"));
        assertTrue(record.getResult().contains("a@example.com"));
        assertEquals(RequestStatus.EXECUTED, req.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"ALLOWED", "APPROVED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("refuses to execute requests that are not ALLOWED or APPROVED")
    void rejectsNonRunnableStatuses(RequestStatus status) {
        ActionRequest req = request(status, ActionType.READ_FILE, null);

        assertThrows(BadRequestException.class, () -> executor.execute(req));
        verify(executionRecordRepository, never()).save(any());
        assertEquals(status, req.getStatus());
    }

    @Test
    @DisplayName("refuses to execute the same request twice")
    void rejectsRepeatedExecution() {
        ActionRequest req = request(RequestStatus.APPROVED, ActionType.DELETE_FILE, Map.of("path", "/tmp/x"));
        when(executionRecordRepository.existsByActionRequestId(req.getId())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> executor.execute(req));
        verify(executionRecordRepository, never()).save(any());
        assertEquals(RequestStatus.APPROVED, req.getStatus());
    }

    @ParameterizedTest
    @EnumSource(ActionType.class)
    @DisplayName("produces a simulated result for every action type, with null parameters")
    void simulatesEveryActionType(ActionType type) {
        ActionRequest req = request(RequestStatus.ALLOWED, type, null);
        when(executionRecordRepository.existsByActionRequestId(req.getId())).thenReturn(false);
        when(executionRecordRepository.save(any(ExecutionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutionRecord record = executor.execute(req);

        assertNotNull(record.getResult());
        assertTrue(record.getResult().startsWith("SIMULATED:"));
    }
}
