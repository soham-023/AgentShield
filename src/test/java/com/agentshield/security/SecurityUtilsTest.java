package com.agentshield.security;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.exception.UnauthorizedException;
import com.agentshield.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityUtilsTest {

    private UserRepository userRepository;
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        securityUtils = new SecurityUtils(userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    @Test
    @DisplayName("Returns the signed-in user")
    void returnsAuthenticatedUser() {
        User alice = User.builder().email("alice@example.com").name("Alice").role(UserRole.USER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        signIn("alice@example.com");

        assertThat(securityUtils.getCurrentUser()).isSameAs(alice);
    }

    @Test
    @DisplayName("No authentication is a 401, not the demo user")
    void noAuthenticationThrows() {
        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Anonymous authentication is a 401")
    void anonymousThrows() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("A principal missing from the database is a 401 and nothing is created")
    void unknownUserThrows() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        signIn("ghost@example.com");

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("no longer exists");
        verify(userRepository, never()).save(any());
    }
}
