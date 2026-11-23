package com.agentshield.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("A valid bearer token authenticates the request")
    void validTokenAuthenticates() throws Exception {
        when(jwtService.validateAndGetSubject("good-token")).thenReturn(Optional.of("demo@agentshield.com"));
        when(userDetailsService.loadUserByUsername("demo@agentshield.com")).thenReturn(
                User.withUsername("demo@agentshield.com").password("x").roles("USER").build());

        MockFilterChain chain = run("Bearer good-token");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("demo@agentshield.com", auth.getName());
        assertNotNull(chain.getRequest(), "filter chain must continue");
    }

    @Test
    @DisplayName("An invalid token leaves the request unauthenticated but continues the chain")
    void invalidTokenStaysAnonymous() throws Exception {
        when(jwtService.validateAndGetSubject(anyString())).thenReturn(Optional.empty());

        MockFilterChain chain = run("Bearer bad-token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Non-bearer or missing Authorization headers are ignored")
    void nonBearerHeaderIsIgnored() throws Exception {
        run("Basic ZGVtbzpwdw==");
        run(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("A token for a deleted user leaves the request unauthenticated")
    void tokenForDeletedUserStaysAnonymous() throws Exception {
        when(jwtService.validateAndGetSubject("orphan")).thenReturn(Optional.of("gone@agentshield.com"));
        when(userDetailsService.loadUserByUsername("gone@agentshield.com"))
                .thenThrow(new UsernameNotFoundException("gone"));

        run("Bearer orphan");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
