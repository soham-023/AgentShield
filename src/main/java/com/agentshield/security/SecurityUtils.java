package com.agentshield.security;

import com.agentshield.entity.User;
import com.agentshield.exception.UnauthorizedException;
import com.agentshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the signed-in user for business services.
 *
 * <p>There is no fallback user: with no authentication, an anonymous one, or a
 * principal that no longer exists in the database, {@link #getCurrentUser()}
 * throws {@link UnauthorizedException}, which the API returns as 401.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the user behind the current request.
     *
     * @throws UnauthorizedException if nobody is signed in or the user is gone
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Authentication is required to access this resource");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }
}
