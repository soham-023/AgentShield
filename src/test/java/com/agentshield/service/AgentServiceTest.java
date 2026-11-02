package com.agentshield.service;

import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.UpdateAgentRequest;
import com.agentshield.dto.response.AgentResponse;
import com.agentshield.dto.response.CreateAgentResponse;
import com.agentshield.entity.Agent;
import com.agentshield.entity.User;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.AccessDeniedException;
import com.agentshield.exception.ResourceNotFoundException;
import com.agentshield.repository.AgentRepository;
import com.agentshield.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AgentService agentService;

    private User owner;
    private User otherUser;
    private User admin;
    private Agent agent;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .name("Owner User")
                .email("owner@agentshield.com")
                .role(UserRole.USER)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Other User")
                .email("other@agentshield.com")
                .role(UserRole.USER)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@agentshield.com")
                .role(UserRole.ADMIN)
                .build();

        agentId = UUID.randomUUID();
        agent = Agent.builder()
                .id(agentId)
                .name("Test Agent")
                .description("Test Description")
                .owner(owner)
                .apiKeyHash("existing_hash_123")
                .status(AgentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create agent and return plain API key once")
    void testCreateAgentSuccess() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> {
            Agent saved = invocation.getArgument(0);
            saved.setId(agentId);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CreateAgentRequest request = new CreateAgentRequest("CustomerBot", "Handles customer inquiries");
        CreateAgentResponse response = agentService.createAgent(request);

        assertNotNull(response);
        assertEquals("CustomerBot", response.getName());
        assertNotNull(response.getApiKey());
        assertTrue(response.getApiKey().startsWith("as_live_"));
        verify(agentRepository, times(1)).save(any(Agent.class));
    }

    @Test
    @DisplayName("Should retrieve agent when caller is owner")
    void testGetAgentByIdOwnerSuccess() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));

        AgentResponse response = agentService.getAgentById(agentId);

        assertNotNull(response);
        assertEquals("Test Agent", response.getName());
        assertEquals(owner.getId(), response.getOwnerId());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when caller does not own the agent")
    void testGetAgentByIdAccessDenied() {
        when(securityUtils.getCurrentUser()).thenReturn(otherUser);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThrows(AccessDeniedException.class, () -> agentService.getAgentById(agentId));
    }

    @Test
    @DisplayName("Admin can retrieve any agent")
    void testGetAgentByIdAdminSuccess() {
        when(securityUtils.getCurrentUser()).thenReturn(admin);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));

        AgentResponse response = agentService.getAgentById(agentId);

        assertNotNull(response);
        assertEquals("Test Agent", response.getName());
    }

    @Test
    @DisplayName("Should update agent successfully")
    void testUpdateAgentSuccess() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAgentRequest updateRequest = new UpdateAgentRequest("UpdatedBot", "New description", AgentStatus.DISABLED);
        AgentResponse response = agentService.updateAgent(agentId, updateRequest);

        assertEquals("UpdatedBot", response.getName());
        assertEquals(AgentStatus.DISABLED, response.getStatus());
    }

    @Test
    @DisplayName("Should rotate API key and return new key")
    void testRotateApiKeySuccess() {
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAgentResponse response = agentService.rotateApiKey(agentId);

        assertNotNull(response.getApiKey());
        assertTrue(response.getApiKey().startsWith("as_live_"));
        verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown agent ID")
    void testAgentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(agentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> agentService.getAgentById(unknownId));
    }
}
