package com.agentshield.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/agents/abc-123");
    }

    private void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status, String error, String message) {
        assertEquals(status, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(status.value(), body.getStatus());
        assertEquals(error, body.getError());
        assertEquals(message, body.getMessage());
        assertEquals("/api/v1/agents/abc-123", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("ResourceNotFoundException maps to 404 with the formatted message")
    void notFound() {
        var ex = new ResourceNotFoundException("Agent", "id", "abc-123");
        assertError(handler.handleNotFound(ex, request), HttpStatus.NOT_FOUND,
                "Not Found", "Agent not found with id: 'abc-123'");
    }

    @Test
    @DisplayName("BadRequestException maps to 400")
    void badRequest() {
        assertError(handler.handleBadRequest(new BadRequestException("bad input"), request),
                HttpStatus.BAD_REQUEST, "Bad Request", "bad input");
    }

    @Test
    @DisplayName("AccessDeniedException maps to 403")
    void accessDenied() {
        assertError(handler.handleAccessDenied(new AccessDeniedException("not yours"), request),
                HttpStatus.FORBIDDEN, "Forbidden", "not yours");
    }

    @Test
    @DisplayName("unexpected exceptions map to 500 without leaking their message")
    void unexpected() {
        var response = handler.handleGeneral(new IllegalStateException("db password is hunter2"), request);
        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", "An unexpected error occurred");
        assertFalse(response.getBody().getMessage().contains("hunter2"));
    }

    @Test
    @DisplayName("unreadable request body maps to 400 instead of 500")
    void unreadableBody() {
        var ex = new org.springframework.http.converter.HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type ActionType from String \"SEND_MAIL\"",
                new org.springframework.mock.http.MockHttpInputMessage(new byte[0]));
        assertError(handler.handleUnreadableBody(ex, request), HttpStatus.BAD_REQUEST,
                "Bad Request", "Request body is missing, malformed, or contains an invalid value");
    }

    @Test
    @DisplayName("wrongly typed path parameter maps to 400 naming the parameter")
    void typeMismatch() {
        var ex = new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                "not-a-uuid", java.util.UUID.class, "agentId", null, new IllegalArgumentException("bad uuid"));
        assertError(handler.handleTypeMismatch(ex, request), HttpStatus.BAD_REQUEST,
                "Bad Request", "Invalid value for parameter 'agentId'");
    }

    @Test
    @DisplayName("missing query parameter maps to 400 naming the parameter")
    void missingParameter() {
        var ex = new org.springframework.web.bind.MissingServletRequestParameterException("status", "String");
        assertError(handler.handleMissingParameter(ex, request), HttpStatus.BAD_REQUEST,
                "Bad Request", "Missing required parameter 'status'");
    }
}
