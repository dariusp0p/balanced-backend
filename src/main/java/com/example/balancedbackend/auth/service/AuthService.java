package com.example.balancedbackend.auth.service;

import com.example.balancedbackend.auth.api.dto.AuthResponse;
import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.SignupRequest;
import com.example.balancedbackend.auth.api.dto.SignupResponse;
import com.example.balancedbackend.auth.api.dto.UserResponse;
import com.example.balancedbackend.auth.model.AuthSession;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.InMemorySessionStore;
import com.example.balancedbackend.auth.store.InMemoryUserStore;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.ConflictException;
import com.example.balancedbackend.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final InMemoryUserStore userStore;
    private final InMemorySessionStore sessionStore;
    private final PasswordEncoder passwordEncoder;
    private final long sessionTtlMinutes;

    public AuthService(InMemoryUserStore userStore,
                       InMemorySessionStore sessionStore,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.session-ttl-minutes:480}") long sessionTtlMinutes) {
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.passwordEncoder = passwordEncoder;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirmPassword must match");
        }

        if (userStore.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("A user with this email already exists");
        }

        try {
            User user = userStore.createUser(
                    request.name(),
                    request.email(),
                    passwordEncoder.encode(request.password())
            );
            return new SignupResponse("User registered successfully", toUserResponse(user));
        } catch (IllegalStateException ex) {
            throw new ConflictException("A user with this email already exists");
        }
    }

    public AuthResponse login(LoginRequest request) {
        User user = userStore.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        Instant expiresAt = Instant.now().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        AuthSession session = sessionStore.createSession(user.id(), expiresAt);

        return new AuthResponse(session.token(), "Bearer", session.expiresAt(), toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.id(), user.name(), user.email());
    }
}

