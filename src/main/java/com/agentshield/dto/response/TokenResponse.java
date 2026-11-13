package com.agentshield.dto.response;

/** A bearer token and its lifetime in seconds. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse(token, "Bearer", expiresInSeconds);
    }
}
