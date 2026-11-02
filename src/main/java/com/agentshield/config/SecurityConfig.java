package com.agentshield.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for AgentShield.
 *
 * PHASE 1: We permit ALL requests so we can test our APIs freely.
 * PHASE 5: We'll add JWT authentication, API key auth, and role-based access.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — we're building a stateless REST API, not a browser form app
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless sessions — no server-side session; every request carries its own auth (JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // PHASE 1: Permit everything. We'll lock this down in Phase 5.
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * BCrypt password encoder bean.
     * BCrypt automatically handles salting and is resistant to brute-force attacks.
     * We register it as a bean so Spring can inject it wherever needed.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
