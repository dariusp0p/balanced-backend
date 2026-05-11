package com.example.balancedbackend.users.api;

import com.example.balancedbackend.auth.api.dto.UserResponse;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.auth.store.UserRepository;
import com.example.balancedbackend.auth.service.AuthService;
import com.example.balancedbackend.common.security.SecuritySupport;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SecuritySupport securitySupport;

    public UsersController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SecuritySupport securitySupport
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<UserResponse> users(Authentication authentication) {
        long currentUserId = securitySupport.requireUserId(authentication);
        return userRepository.findAllByOrderByFirstNameAscLastNameAscEmailAsc()
                .stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isAdmin(),
                roleRepository.findRoleNamesByUserId(user.getId()),
                AuthService.toDailyNutritionTargetResponse(user)
        );
    }
}
