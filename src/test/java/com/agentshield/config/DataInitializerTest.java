package com.agentshield.config;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
        initializer = new DataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "demoUserPassword", "user-secret");
        ReflectionTestUtils.setField(initializer, "demoAdminPassword", "admin-secret");
    }

    @Test
    void seedsDemoUserAndAdminWhenMissing() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        initializer.run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        List<User> users = saved.getAllValues();

        assertEquals(DataInitializer.DEMO_USER_EMAIL, users.get(0).getEmail());
        assertEquals(UserRole.USER, users.get(0).getRole());
        assertEquals("hashed:user-secret", users.get(0).getPasswordHash());

        assertEquals(DataInitializer.DEMO_ADMIN_EMAIL, users.get(1).getEmail());
        assertEquals(UserRole.ADMIN, users.get(1).getRole());
        assertEquals("hashed:admin-secret", users.get(1).getPasswordHash());
    }

    @Test
    void doesNothingWhenDemoAccountsAlreadyExist() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void seedsOnlyTheMissingAccount() {
        when(userRepository.existsByEmail(DataInitializer.DEMO_USER_EMAIL)).thenReturn(true);
        when(userRepository.existsByEmail(DataInitializer.DEMO_ADMIN_EMAIL)).thenReturn(false);

        initializer.run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(DataInitializer.DEMO_ADMIN_EMAIL, saved.getValue().getEmail());
    }
}
