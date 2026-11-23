package com.agentshield.audit;

import com.agentshield.entity.AuditLog;
import com.agentshield.enums.AuditEventType;
import com.agentshield.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("recordEvent saves an entry carrying every field it was given")
    void recordEventSavesAllFields() {
        UUID actor = UUID.randomUUID();
        UUID agent = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = auditService.recordEvent(actor, agent, request,
                AuditEventType.ACTION_APPROVED, "APPROVED", Map.of("actionType", "SEND_EMAIL"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog entry = captor.getValue();
        assertSame(entry, saved);
        assertEquals(actor, entry.getActorId());
        assertEquals(agent, entry.getAgentId());
        assertEquals(request, entry.getActionRequestId());
        assertEquals(AuditEventType.ACTION_APPROVED, entry.getEventType());
        assertEquals("APPROVED", entry.getResult());
        assertEquals("SEND_EMAIL", entry.getMetadata().get("actionType"));
    }

    @Test
    @DisplayName("Events without an action request or metadata are still recorded")
    void recordEventAllowsMissingOptionalFields() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = auditService.recordEvent(UUID.randomUUID(), UUID.randomUUID(), null,
                AuditEventType.AGENT_CREATED, "CREATED", null);

        assertNull(saved.getActionRequestId());
        assertNull(saved.getMetadata());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}
