package com.example.balancedbackend.auth.service;

import com.example.balancedbackend.auth.api.dto.AuthResponse;
import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.SignupRequest;
import com.example.balancedbackend.auth.api.dto.SignupResponse;
import com.example.balancedbackend.auth.api.dto.UserResponse;
import com.example.balancedbackend.auth.model.AuthSession;
import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.model.UserRole;
import com.example.balancedbackend.auth.store.InMemorySessionStore;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.auth.store.UserRepository;
import com.example.balancedbackend.auth.store.UserRoleRepository;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.ConflictException;
import com.example.balancedbackend.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final InMemorySessionStore sessionStore;
    private final PasswordEncoder passwordEncoder;
    private final long sessionTtlMinutes;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       InMemorySessionStore sessionStore,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.session-ttl-minutes:480}") long sessionTtlMinutes) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionStore = sessionStore;
        this.passwordEncoder = passwordEncoder;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirmPassword must match");
        }

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("A user with this email already exists");
        }

        User user = new User(request.name(), normalizedEmail, passwordEncoder.encode(request.password()));
        userRepository.save(user);
        assignDefaultRole(user);
        return new SignupResponse("User registered successfully", toUserResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        Instant expiresAt = Instant.now().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        AuthSession session = sessionStore.createSession(user.getId(), expiresAt);

        return new AuthResponse(session.token(), "Bearer", session.expiresAt(), toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isAdmin(),
                roleRepository.findRoleNamesByUserId(user.getId())
        );
    }

    private void assignDefaultRole(User user) {
        Role role = roleRepository.findByName(user.isAdmin() ? "ADMIN" : "USER")
                .orElse(null);
        if (role == null) return;

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
