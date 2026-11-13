package com.agentshield.exception;

/**
 * Thrown when an operation needs a signed-in user and there is none
 * (or the token names a user that no longer exists). Returns HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
