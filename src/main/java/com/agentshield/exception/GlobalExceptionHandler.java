package com.agentshield.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Global exception handler.
 *
 * @RestControllerAdvice catches exceptions from ALL controllers and
 * converts them into our standard ErrorResponse format.
 *
 * Without this, Spring returns ugly default error pages.
 * With this, every error looks like:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Agent not found with id: abc-123",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "path": "/api/v1/agents/abc-123"
 * }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        log.warn("Bad request: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles service calls that need a signed-in user when there is none.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {

        log.warn("Unauthorized at {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles failed logins (wrong email or password). The message is kept
     * generic so it does not reveal which of the two was wrong.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid email or password")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Extracts individual field errors and returns them in a structured format.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields have invalid values")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles request bodies that cannot be parsed: malformed JSON, or a value
     * that does not fit the target type (for example an unknown actionType).
     * Without this, such requests fall through to the catch-all and return 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return badRequest("Request body is missing, malformed, or contains an invalid value", request);
    }

    /**
     * Handles path or query values of the wrong type, such as a non-UUID agentId.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch for parameter '{}' at {}", ex.getName(), request.getRequestURI());
        return badRequest("Invalid value for parameter '" + ex.getName() + "'", request);
    }

    /**
     * Handles a required query parameter that was not supplied.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return badRequest("Missing required parameter '" + ex.getParameterName() + "'", request);
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Returns 500 Internal Server Error.
     * Logs the full stack trace for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
