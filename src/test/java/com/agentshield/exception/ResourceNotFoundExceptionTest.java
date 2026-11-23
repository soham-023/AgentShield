package com.agentshield.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("The resource/field/value constructor builds a readable message")
    void formatsResourceFieldValueMessage() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ResourceNotFoundException ex = new ResourceNotFoundException("Agent", "id", id);

        assertEquals("Agent not found with id: '00000000-0000-0000-0000-000000000001'", ex.getMessage());
    }

    @Test
    @DisplayName("Custom exceptions map to the documented HTTP statuses")
    void exceptionsCarryExpectedStatuses() {
        assertEquals(HttpStatus.NOT_FOUND,
                ResourceNotFoundException.class.getAnnotation(ResponseStatus.class).value());
        assertEquals(HttpStatus.BAD_REQUEST,
                BadRequestException.class.getAnnotation(ResponseStatus.class).value());
        assertEquals(HttpStatus.FORBIDDEN,
                AccessDeniedException.class.getAnnotation(ResponseStatus.class).value());
    }
}
