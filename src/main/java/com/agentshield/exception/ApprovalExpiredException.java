package com.agentshield.exception;

/**
 * Raised when an approval expires during a decision attempt.
 */
public class ApprovalExpiredException extends BadRequestException {

    public ApprovalExpiredException(String message) {
        super(message);
    }
}
