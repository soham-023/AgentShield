package com.agentshield.security;

import com.agentshield.entity.User;
import com.agentshield.enums.UserRole;
import com.agentshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to identify the current user in business services.
 *
 * In Phases 2-4 (prior to JWT): If no JWT principal is present in the SecurityContext,
 * it falls back to the default demo user so all CRUD operations can be tested cleanly.
 * In Phase 5: Once JWT is active, it extracts the authenticated user from the SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public static final String DEFAULT_USER_EMAIL = "demo@agentshield.com";

    /**
     * Resolves the current user.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            return userRepository.findByEmail(email)
                    .orElseGet(this::getDefaultUser);
        }

        return getDefaultUser();
    }

    private User getDefaultUser() {
        return userRepository.findByEmail(DEFAULT_USER_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Demo User")
                        .email(DEFAULT_USER_EMAIL)
                        .passwordHash("$2a$10$demoHashedPasswordPlaceholder")
                        .role(UserRole.USER)
                        .build()));
    }
}
