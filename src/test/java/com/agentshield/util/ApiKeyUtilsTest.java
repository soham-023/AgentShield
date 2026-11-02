package com.agentshield.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyUtilsTest {

    @Test
    @DisplayName("Should generate key with correct prefix and entropy")
    void testGenerateApiKey() {
        String key = ApiKeyUtils.generateApiKey();

        assertNotNull(key);
        assertTrue(key.startsWith("as_live_"), "Key should start with 'as_live_'");
        // "as_live_" (8 chars) + 64 hex chars (32 bytes * 2) = 72 chars
        assertEquals(72, key.length(), "Key length should be 72 characters");
    }

    @Test
    @DisplayName("Generated keys should be unique")
    void testUniqueKeys() {
        String key1 = ApiKeyUtils.generateApiKey();
        String key2 = ApiKeyUtils.generateApiKey();

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("Should hash API key consistently using SHA-256")
    void testHashApiKey() {
        String key = "as_live_testkey123456789";
        String hash1 = ApiKeyUtils.hashApiKey(key);
        String hash2 = ApiKeyUtils.hashApiKey(key);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex string should be 64 characters");
        assertEquals(hash1, hash2, "Hashing same key twice must produce identical hash");
    }

    @Test
    @DisplayName("Hashing should fail for blank or null key")
    void testHashApiKeyInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ApiKeyUtils.hashApiKey(null));
        assertThrows(IllegalArgumentException.class, () -> ApiKeyUtils.hashApiKey("   "));
    }
}
