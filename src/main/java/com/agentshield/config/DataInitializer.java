package com.agentshield.config;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a demo user and a demo admin on startup if they don't already exist,
 * so a fresh local instance can be used straight away.
 *
 * <p>Seeding is controlled by {@code agentshield.seed.demo-users} (env
 * {@code AGENTSHIELD_SEED_DEMO_USERS}); set it to {@code false} in any shared or
 * production environment. The demo passwords can be overridden with
 * {@code AGENTSHIELD_DEMO_USER_PASSWORD} and {@code AGENTSHIELD_DEMO_ADMIN_PASSWORD}.
 */
@Component
@ConditionalOnProperty(prefix = "agentshield.seed", name = "demo-users", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    public static final String DEMO_USER_EMAIL = "demo@agentshield.com";
    public static final String DEMO_ADMIN_EMAIL = "admin@agentshield.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${agentshield.seed.demo-user-password:Password123!}")
    private String demoUserPassword;

    @Value("${agentshield.seed.demo-admin-password:AdminPass123!}")
    private String demoAdminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(DEMO_USER_EMAIL)) {
            User demoUser = User.builder()
                    .name("Demo User")
                    .email(DEMO_USER_EMAIL)
                    .passwordHash(passwordEncoder.encode(demoUserPassword))
                    .role(UserRole.USER)
                    .build();
            userRepository.save(demoUser);
            log.info("Initialized default user: {} (Role: USER)", DEMO_USER_EMAIL);
        }

        if (!userRepository.existsByEmail(DEMO_ADMIN_EMAIL)) {
            User adminUser = User.builder()
                    .name("System Admin")
                    .email(DEMO_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(demoAdminPassword))
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(adminUser);
            log.info("Initialized default admin: {} (Role: ADMIN)", DEMO_ADMIN_EMAIL);
        }
    }
}
