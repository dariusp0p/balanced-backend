package com.example.balancedbackend.auth.service;

import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.SignupRequest;
import com.example.balancedbackend.auth.store.InMemorySessionStore;
import com.example.balancedbackend.auth.store.InMemoryUserStore;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.ConflictException;
import com.example.balancedbackend.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                new InMemoryUserStore(),
                new InMemorySessionStore(),
                new BCryptPasswordEncoder(),
                120
        );
    }

    @Test
    void signupShouldCreateUser() {
        var response = authService.signup(new SignupRequest("Darius", "darius@example.com", "password123", "password123"));

        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.user().email()).isEqualTo("darius@example.com");
    }

    @Test
    void signupShouldFailWhenPasswordsDoNotMatch() {
        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("Darius", "darius@example.com", "password123", "passwordABC"))
        ).isInstanceOf(BadRequestException.class)
                .hasMessage("Password and confirmPassword must match");
    }

    @Test
    void signupShouldFailWhenEmailAlreadyExists() {
        authService.signup(new SignupRequest("Darius", "darius@example.com", "password123", "password123"));

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("Darius 2", "darius@example.com", "password456", "password456"))
        ).isInstanceOf(ConflictException.class);
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        authService.signup(new SignupRequest("Darius", "darius@example.com", "password123", "password123"));

        var response = authService.login(new LoginRequest("darius@example.com", "password123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginShouldFailForInvalidCredentials() {
        authService.signup(new SignupRequest("Darius", "darius@example.com", "password123", "password123"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("darius@example.com", "wrongpass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }
}

