package com.example.balancedbackend.auth.service;

import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.auth.api.dto.DailyNutritionTargetRequest;
import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.SignupRequest;
import com.example.balancedbackend.auth.model.AuthSession;
import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.InMemorySessionStore;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.auth.store.UserRepository;
import com.example.balancedbackend.auth.store.UserRoleRepository;
import com.example.balancedbackend.shared.exception.BadRequestException;
import com.example.balancedbackend.shared.exception.ConflictException;
import com.example.balancedbackend.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private InMemorySessionStore sessionStore;
    @Mock
    private AuditService auditService;
    @Mock
    private AuthProtectionService authProtectionService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                sessionStore,
                auditService,
                authProtectionService,
                passwordEncoder,
                480
        );
    }

    @Test
    void signupShouldFailWhenPasswordsDoNotMatch() {
        SignupRequest request = new SignupRequest(
                "User",
                "user@example.com",
                "secret123",
                "different",
                "What is your favorite food?",
                "Pizza"
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("confirmPassword");
    }

    @Test
    void signupShouldFailWhenEmailAlreadyExistsCaseInsensitive() {
        SignupRequest request = new SignupRequest(
                "User",
                "USER@example.com",
                "secret123",
                "secret123",
                "What is your favorite food?",
                "Pizza"
        );
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void signupShouldCreateUserAssignRoleAndLogAction() {
        SignupRequest request = new SignupRequest(
                "Alice",
                "Alice@Example.com",
                "secret123",
                "secret123",
                "What is your favorite food?",
                "Pizza"
        );
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(passwordEncoder.encode("pizza")).thenReturn("encoded-answer");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        }).when(userRepository).save(any(User.class));
        when(sessionStore.createSession(eq(42L), any(Instant.class)))
                .thenReturn(new AuthSession("signup-token", 42L, Instant.now().plusSeconds(3600)));
        Role userRole = new Role();
        userRole.setId(7L);
        userRole.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findRoleNamesByUserId(42L)).thenReturn(List.of("USER"));

        var response = authService.signup(request);

        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().roles()).containsExactly("USER");
        assertThat(response.user().dailyNutritionTarget().calories()).isEqualTo(2000);
        assertThat(response.token()).isEqualTo("signup-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getRecoveryQuestion()).isEqualTo("What is your favorite food?");
        assertThat(userCaptor.getValue().getRecoveryAnswerHash()).isEqualTo("encoded-answer");
        verify(userRoleRepository).save(any());
        verify(auditService).logAction(42L, "Signed up with email alice@example.com");
    }

    @Test
    void loginShouldReturnBearerTokenAndUserDetails() {
        User user = new User("Test User", "test@example.com", "hashed");
        user.setId(5L);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(sessionStore.createSession(eq(5L), any(Instant.class)))
                .thenReturn(new AuthSession("token-123", 5L, expiresAt));
        when(roleRepository.findRoleNamesByUserId(5L)).thenReturn(List.of("USER"));

        var response = authService.login(new LoginRequest("Test@Example.com", "secret123"), "127.0.0.1");

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(5L);
        assertThat(response.user().email()).isEqualTo("test@example.com");
        verify(authProtectionService).ensureLoginAllowed("127.0.0.1", "test@example.com");
        verify(authProtectionService).clearLoginFailures("127.0.0.1", "test@example.com");
        verify(auditService).logAction(5L, "Logged in");
    }

    @Test
    void loginShouldFailForUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "secret123"), "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginShouldFailForWrongPassword() {
        User user = new User("User", "user@example.com", "hashed");
        user.setId(6L);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong"), "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(sessionStore, never()).createSession(any(Long.class), any(Instant.class));
    }

    @Test
    void recoverPasswordShouldReplacePasswordAndInvalidateSessions() {
        User user = new User("Recovery User", "recover@example.com", "old-hash");
        user.setId(12L);
        user.setRecoveryQuestion("What is your favorite food?");
        user.setRecoveryAnswerHash("answer-hash");
        when(userRepository.findByEmailIgnoreCase("recover@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pizza", "answer-hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hash");
        when(roleRepository.findRoleNamesByUserId(12L)).thenReturn(List.of("USER"));
        when(sessionStore.createSession(eq(12L), any(Instant.class)))
                .thenReturn(new AuthSession("recovery-token", 12L, Instant.now().plusSeconds(3600)));

        var response = authService.recoverPassword(
                new com.example.balancedbackend.auth.api.dto.PasswordRecoveryResetRequest(
                        "recover@example.com",
                        "Pizza",
                        "newpass123",
                        "newpass123"
                ),
                "127.0.0.1"
        );

        assertThat(response.token()).isEqualTo("recovery-token");
        verify(userRepository).save(user);
        verify(sessionStore).invalidateAllForUser(12L);
        verify(authProtectionService).clearRecoveryFailures("127.0.0.1", "recover@example.com");
        verify(auditService).logAction(12L, "Recovered password and created a new session");
    }

    @Test
    void updateDailyNutritionTargetShouldPersistAndLog() {
        User user = new User("User", "user@example.com", "hashed");
        user.setId(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        DailyNutritionTargetRequest request = new DailyNutritionTargetRequest(2200, 170, 240, 75);

        var response = authService.updateDailyNutritionTarget(11L, request);

        assertThat(response.calories()).isEqualTo(2200);
        assertThat(response.protein()).isEqualTo(170);
        assertThat(response.carbs()).isEqualTo(240);
        assertThat(response.fats()).isEqualTo(75);
        verify(userRepository).save(user);
        verify(auditService).logAction(
                11L,
                "Updated daily nutrition target to calories=2200.0, protein=170.0, carbs=240.0, fats=75.0"
        );
    }

    @Test
    void updateDailyNutritionTargetShouldFailForMissingUser() {
        when(userRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateDailyNutritionTarget(
                88L,
                new DailyNutritionTargetRequest(2000, 150, 250, 70)
        )).isInstanceOf(UnauthorizedException.class);
    }
}
