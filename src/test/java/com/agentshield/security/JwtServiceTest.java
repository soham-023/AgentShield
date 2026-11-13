package com.agentshield.security;

import com.agentshield.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60_000));

    @Test
    @DisplayName("A generated token round-trips to its subject")
    void roundTrip() {
        String token = jwtService.generateToken("alice@example.com");
        assertThat(jwtService.validateAndGetSubject(token)).contains("alice@example.com");
    }

    @Test
    @DisplayName("Tokens signed with another secret are rejected")
    void rejectsForeignSignature() {
        JwtService other = new JwtService(new JwtProperties("a-different-secret-that-is-also-32-bytes+", 60_000));
        assertThat(jwtService.validateAndGetSubject(other.generateToken("alice@example.com"))).isEmpty();
    }

    @Test
    @DisplayName("Expired tokens are rejected")
    void rejectsExpired() throws InterruptedException {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, 1));
        String token = shortLived.generateToken("alice@example.com");
        Thread.sleep(1100); // JWT expiry has one-second resolution
        assertThat(shortLived.validateAndGetSubject(token)).isEmpty();
    }

    @Test
    @DisplayName("Garbage input is rejected without throwing")
    void rejectsGarbage() {
        assertThat(jwtService.validateAndGetSubject("not.a.jwt")).isEmpty();
        assertThat(jwtService.validateAndGetSubject("")).isEmpty();
    }
}
