package com.agentshield.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simple health check endpoint.
 * Used to verify the application is running and can respond to requests.
 *
 * Try it: GET http://localhost:8080/api/v1/health
 */
@RestController
public class HealthController {

    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "AgentShield",
            "version", "0.0.1-SNAPSHOT",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
