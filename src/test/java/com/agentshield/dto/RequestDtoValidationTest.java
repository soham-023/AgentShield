package com.agentshield.dto;

import com.agentshield.dto.request.ApprovalDecisionRequest;
import com.agentshield.dto.request.CreateAgentRequest;
import com.agentshield.dto.request.CreatePolicyRequest;
import com.agentshield.dto.request.LoginRequest;
import com.agentshield.dto.request.UpdateAgentRequest;
import com.agentshield.enums.ActionType;
import com.agentshield.enums.AgentStatus;
import com.agentshield.enums.PermissionDecision;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bean-validation rules for the request bodies other than SubmitActionRequest. */
class RequestDtoValidationTest {

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

    private static <T> Set<String> invalidFields(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void createAgentRequiresNameOfAtLeastTwoCharacters() {
        assertTrue(invalidFields(CreateAgentRequest.builder().name("Reporter").build()).isEmpty());
        assertEquals(Set.of("name"), invalidFields(CreateAgentRequest.builder().build()));
        assertEquals(Set.of("name"), invalidFields(CreateAgentRequest.builder().name("x").build()));
    }

    @Test
    void createAgentRejectsOverlongDescription() {
        CreateAgentRequest request = CreateAgentRequest.builder()
                .name("Reporter").description("d".repeat(501)).build();
        assertEquals(Set.of("description"), invalidFields(request));
    }

    @Test
    void updateAgentRequiresNameAndStatus() {
        assertTrue(invalidFields(UpdateAgentRequest.builder()
                .name("Reporter").status(AgentStatus.ACTIVE).build()).isEmpty());
        assertEquals(Set.of("name", "status"), invalidFields(UpdateAgentRequest.builder().build()));
    }

    @Test
    void createPolicyRequiresActionTypeAndDecision() {
        assertTrue(invalidFields(CreatePolicyRequest.builder()
                .actionType(ActionType.SEND_EMAIL).decision(PermissionDecision.ALLOW).build()).isEmpty());
        assertEquals(Set.of("actionType", "decision"), invalidFields(CreatePolicyRequest.builder().build()));
    }

    @Test
    void approvalCommentsAreOptionalButCapped() {
        assertTrue(invalidFields(new ApprovalDecisionRequest()).isEmpty());
        assertEquals(Set.of("comments"), invalidFields(new ApprovalDecisionRequest("c".repeat(501))));
    }

    @Test
    void loginRequiresValidEmailAndPassword() {
        assertTrue(invalidFields(new LoginRequest("demo@agentshield.com", "pw")).isEmpty());
        assertEquals(Set.of("email", "password"), invalidFields(new LoginRequest("", " ")));
        assertEquals(Set.of("email"), invalidFields(new LoginRequest("not-an-email", "pw")));
    }

    @Test
    void loginMessagesAreHumanReadable() {
        Set<String> messages = validator.validate(new LoginRequest("not-an-email", ""))
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertEquals(Set.of("Email must be a valid email address", "Password is required"), messages);
    }
}
