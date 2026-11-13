package com.agentshield.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String VALID_SECRET = "a".repeat(JwtProperties.MIN_SECRET_BYTES);

    @Test
    @DisplayName("Missing secret fails with a message naming the env var")
    void missingSecretFails() {
        assertThatThrownBy(() -> new JwtProperties(null, 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AGENTSHIELD_JWT_SECRET is not set");
        assertThatThrownBy(() -> new JwtProperties("   ", 1000))
                .hasMessageContaining("AGENTSHIELD_JWT_SECRET is not set");
    }

    @Test
    @DisplayName("Secret shorter than 32 bytes is rejected")
    void shortSecretFails() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(31), 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short (31 bytes)");
    }

    @Test
    @DisplayName("Secret length is measured in bytes, not characters")
    void secretLengthCountsBytes() {
        // 16 two-byte characters = 32 bytes
        assertThat(new JwtProperties("é".repeat(16), 1000).secret()).hasSize(16);
    }

    @Test
    @DisplayName("Non-positive expiration is rejected")
    void nonPositiveExpirationFails() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, 0))
                .hasMessageContaining("expiration-ms must be positive");
    }

    @Test
    @DisplayName("toString never prints the secret")
    void toStringMasksSecret() {
        JwtProperties props = new JwtProperties(VALID_SECRET, 1000);
        assertThat(props.toString()).doesNotContain(VALID_SECRET).contains("****");
    }
}
