package com.agentshield.security;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("A known email loads the user with its password hash and ROLE_ authority")
    void loadsKnownUser() {
        User user = User.builder().id(UUID.randomUUID()).name("Admin")
                .email("admin@agentshield.com").passwordHash("{bcrypt}hash").role(UserRole.ADMIN).build();
        when(userRepository.findByEmail("admin@agentshield.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin@agentshield.com");

        assertEquals("admin@agentshield.com", details.getUsername());
        assertEquals("{bcrypt}hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }

    @Test
    @DisplayName("An unknown email throws UsernameNotFoundException")
    void unknownEmailThrows() {
        when(userRepository.findByEmail("nobody@agentshield.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nobody@agentshield.com"));
    }
}
