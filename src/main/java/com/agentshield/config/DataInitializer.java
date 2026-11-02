package com.agentshield.config;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds initial users into the database on startup if they don't already exist.
 * This makes testing immediately available without having to manually seed users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("demo@agentshield.com")) {
            User demoUser = User.builder()
                    .name("Demo User")
                    .email("demo@agentshield.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .role(UserRole.USER)
                    .build();
            userRepository.save(demoUser);
            log.info("Initialized default user: demo@agentshield.com (Role: USER)");
        }

        if (!userRepository.existsByEmail("admin@agentshield.com")) {
            User adminUser = User.builder()
                    .name("System Admin")
                    .email("admin@agentshield.com")
                    .passwordHash(passwordEncoder.encode("AdminPass123!"))
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(adminUser);
            log.info("Initialized default admin: admin@agentshield.com (Role: ADMIN)");
        }
    }
}
