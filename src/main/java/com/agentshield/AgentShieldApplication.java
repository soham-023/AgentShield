package com.agentshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the AgentShield application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration:       marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration: tells Spring Boot to auto-configure based on dependencies
 *   - @ComponentScan:       scans the com.agentshield package for components
 */
@SpringBootApplication
public class AgentShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentShieldApplication.class, args);
    }
}
