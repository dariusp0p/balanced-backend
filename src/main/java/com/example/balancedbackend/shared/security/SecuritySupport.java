package com.example.balancedbackend.shared.security;

import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.UserRepository;
import com.example.balancedbackend.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {
    private final UserRepository userRepository;

    public SecuritySupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }

    public User requireUser(Authentication authentication) {
        long userId = requireUserId(authentication);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
    }

    public User requireAdmin(Authentication authentication) {
        User user = requireUser(authentication);
        if (!user.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }
        return user;
    }
}
