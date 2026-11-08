package com.agentshield.dto;

import com.agentshield.dto.request.SubmitActionRequest;
import com.agentshield.enums.ActionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitActionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @Test
    void validRequestHasNoViolations() {
        SubmitActionRequest request = SubmitActionRequest.builder()
                .actionType(ActionType.SEND_EMAIL)
                .description("Email the weekly report")
                .idempotencyKey("report-2026-11-08")
                .build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void missingActionTypeIsRejected() {
        SubmitActionRequest request = SubmitActionRequest.builder().build();

        Set<ConstraintViolation<SubmitActionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("actionType", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void overlongDescriptionIsRejected() {
        SubmitActionRequest request = SubmitActionRequest.builder()
                .actionType(ActionType.READ_FILE)
                .description("x".repeat(501))
                .build();

        Set<ConstraintViolation<SubmitActionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void overlongIdempotencyKeyIsRejected() {
        SubmitActionRequest request = SubmitActionRequest.builder()
                .actionType(ActionType.READ_FILE)
                .idempotencyKey("k".repeat(256))
                .build();

        Set<ConstraintViolation<SubmitActionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("idempotencyKey", violations.iterator().next().getPropertyPath().toString());
    }
}
