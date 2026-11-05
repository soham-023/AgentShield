package com.agentshield.exception;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to access a resource they don't own.
 * Returns HTTP 403 (Forbidden).
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
    
public class AccessDeniedException extends RuntimeException 
{

    public AccessDeniedException(String message) {
        super(message);
    }
}
