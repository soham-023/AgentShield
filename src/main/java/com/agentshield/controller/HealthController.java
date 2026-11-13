package com.agentshield.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Liveness check, public (no authentication) so load balancers and
 * container health checks can call it: {@code GET /health} or {@code GET /api/v1/health}.
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
