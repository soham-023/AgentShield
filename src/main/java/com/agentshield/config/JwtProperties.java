package com.agentshield.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * JWT settings bound from {@code agentshield.jwt.*}.
 *
 * <p>The signing secret has no default and is checked when the application
 * starts, so a missing or weak secret stops startup with a clear message
 * instead of surfacing later as a signing error or, worse, as tokens signed
 * with a key anyone can read in the repository.
 *
 * @param secret       HMAC signing secret, from {@code AGENTSHIELD_JWT_SECRET}
 * @param expirationMs token lifetime in milliseconds
 */
@ConfigurationProperties(prefix = "agentshield.jwt")
public record JwtProperties(String secret, long expirationMs) {

    /** HMAC-SHA256 needs a key of at least 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "AGENTSHIELD_JWT_SECRET is not set. Set it to a random value of at least "
                            + MIN_SECRET_BYTES + " bytes, for example: openssl rand -base64 48");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "AGENTSHIELD_JWT_SECRET is too short (" + length + " bytes). It must be at least "
                            + MIN_SECRET_BYTES + " bytes (256 bits) for HMAC-SHA256.");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("agentshield.jwt.expiration-ms must be positive, was " + expirationMs);
        }
    }

    /** Keeps the secret out of logs and error messages. */
    @Override
    public String toString() {
        return "JwtProperties[secret=****, expirationMs=" + expirationMs + "]";
    }
}
