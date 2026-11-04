package com.agentshield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring application context loads successfully.
 *
 * @SpringBootTest boots the full application context.
 * @ActiveProfiles("test") — not used yet but ready for profile-based config.
 *
 * If this test passes, it means:
 *   - All beans are wired correctly
 *   - All entities are valid
 *   - All repositories are functional
 *   - Database connection (H2 for tests) works
 */
@SpringBootTest
class AgentShieldApplicationTests {
    

    @Test
    void contextLoads() {
        // If the context loads without errors, this test passes.
    }
}
