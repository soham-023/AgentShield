package com.agentshield.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility for generating and hashing API keys.
 *
 * Security Best Practices:
 * 1. Uses SecureRandom instead of Random for cryptographically strong randomness.
 * 2. Prefix "as_live_" allows easy identification of keys in logs or secrets scanners.
 * 3. SHA-256 one-way hashing ensures the raw key is never stored in the database.
 */
public final class ApiKeyUtils {

    private static final String KEY_PREFIX = "as_live_";
    private static final int KEY_BYTE_LENGTH = 32; // 256 bits of entropy
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ApiKeyUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Generates a new cryptographically secure API key.
     * Example: "as_live_a1b2c3d4e5f6..."
     */
    public static String generateApiKey() {
        byte[] randomBytes = new byte[KEY_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String hex = HexFormat.of().formatHex(randomBytes);
        return KEY_PREFIX + hex;
    }

    /**
     * Computes the SHA-256 hash of an API key as a hexadecimal string.
     * This hash is what gets stored in the database.
     */
    public static String hashApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }
}
