package com.agentshield.config;

import com.agentshield.security.JwtAuthenticationFilter;
import com.agentshield.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security configuration for AgentShield.
 *
 * <p>Public: health checks, login, API docs, the static dashboard, and
 * {@code POST /api/v1/agent-actions} (agents authenticate there with their
 * {@code X-Agent-API-Key}, checked by the service). Everything else under the
 * API needs a user, authenticated by a JWT bearer token from
 * {@code POST /api/v1/auth/login} or by HTTP Basic (used by the dashboard).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String[] PUBLIC_GET_PATHS = {
            "/", "/index.html", "/favicon.ico",
            "/health", "/api/v1/health",
            "/api-docs", "/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
            // Stateless REST API: no cookies-based session, so CSRF protection is not needed
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/agent-actions").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Answers unauthenticated requests with a JSON 401 in the same shape as
     * other API errors. The Basic challenge lets the bundled dashboard prompt
     * for credentials.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "Unauthorized");
            body.put("message", "Authentication is required to access this resource");
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("path", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Basic realm=\"AgentShield\"");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * BCrypt password encoder; salts each hash and is deliberately slow
     * to resist brute force.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
