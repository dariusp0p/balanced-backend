package com.example.balancedbackend.auth.service;

import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.auth.api.dto.AuthResponse;
import com.example.balancedbackend.auth.api.dto.DailyNutritionTargetRequest;
import com.example.balancedbackend.auth.api.dto.DailyNutritionTargetResponse;
import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.PasswordRecoveryQuestionRequest;
import com.example.balancedbackend.auth.api.dto.PasswordRecoveryQuestionResponse;
import com.example.balancedbackend.auth.api.dto.PasswordRecoveryResetRequest;
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
import com.example.balancedbackend.shared.exception.BadRequestException;
import com.example.balancedbackend.shared.exception.ConflictException;
import com.example.balancedbackend.shared.exception.UnauthorizedException;
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
    private final AuditService auditService;
    private final AuthProtectionService authProtectionService;
    private final long sessionTtlMinutes;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       InMemorySessionStore sessionStore,
                       AuditService auditService,
                       AuthProtectionService authProtectionService,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.session-ttl-minutes:480}") long sessionTtlMinutes) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionStore = sessionStore;
        this.auditService = auditService;
        this.authProtectionService = authProtectionService;
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
        user.setRecoveryQuestion(normalizeText(request.recoveryQuestion()));
        user.setRecoveryAnswerHash(passwordEncoder.encode(normalizeRecoveryAnswer(request.recoveryAnswer())));
        userRepository.save(user);
        assignDefaultRole(user);
        auditService.logAction(user.getId(), "Signed up with email " + user.getEmail());
        AuthSession session = createSession(user.getId());
        return new SignupResponse(
                "User registered successfully",
                session.token(),
                "Bearer",
                session.expiresAt(),
                toUserResponse(user)
        );
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.email());
        authProtectionService.ensureLoginAllowed(clientIp, normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    authProtectionService.recordFailedLogin(clientIp, normalizedEmail);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            authProtectionService.recordFailedLogin(clientIp, normalizedEmail);
            throw new UnauthorizedException("Invalid email or password");
        }

        authProtectionService.clearLoginFailures(clientIp, normalizedEmail);
        AuthSession session = createSession(user.getId());
        auditService.logAction(user.getId(), "Logged in");

        return new AuthResponse(session.token(), "Bearer", session.expiresAt(), toUserResponse(user));
    }

    public PasswordRecoveryQuestionResponse getRecoveryQuestion(PasswordRecoveryQuestionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("No recovery profile found for that email"));

        if (!user.hasRecoveryProfile()) {
            throw new UnauthorizedException("No recovery profile found for that email");
        }

        return new PasswordRecoveryQuestionResponse(
                "Recovery question loaded",
                user.getRecoveryQuestion()
        );
    }

    public AuthResponse recoverPassword(PasswordRecoveryResetRequest request, String clientIp) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirmPassword must match");
        }

        String normalizedEmail = normalizeEmail(request.email());
        authProtectionService.ensureRecoveryAllowed(clientIp, normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    authProtectionService.recordFailedRecovery(clientIp, normalizedEmail);
                    return new UnauthorizedException("Invalid recovery answer");
                });

        if (!user.hasRecoveryProfile()
                || !passwordEncoder.matches(
                normalizeRecoveryAnswer(request.recoveryAnswer()),
                user.getRecoveryAnswerHash())
        ) {
            authProtectionService.recordFailedRecovery(clientIp, normalizedEmail);
            throw new UnauthorizedException("Invalid recovery answer");
        }

        authProtectionService.clearRecoveryFailures(clientIp, normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        sessionStore.invalidateAllForUser(user.getId());

        AuthSession session = createSession(user.getId());
        auditService.logAction(user.getId(), "Recovered password and created a new session");
        return new AuthResponse(session.token(), "Bearer", session.expiresAt(), toUserResponse(user));
    }

    public DailyNutritionTargetResponse updateDailyNutritionTarget(long userId, DailyNutritionTargetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));

        user.setDailyCalorieTarget(request.calories());
        user.setDailyProteinTarget(request.protein());
        user.setDailyCarbsTarget(request.carbs());
        user.setDailyFatsTarget(request.fats());
        userRepository.save(user);

        auditService.logAction(
                userId,
                "Updated daily nutrition target to calories=" + request.calories()
                        + ", protein=" + request.protein()
                        + ", carbs=" + request.carbs()
                        + ", fats=" + request.fats()
        );

        return toDailyNutritionTargetResponse(user);
    }

    public DailyNutritionTargetResponse getDailyNutritionTarget(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
        return toDailyNutritionTargetResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isAdmin(),
                roleRepository.findRoleNamesByUserId(user.getId()),
                toDailyNutritionTargetResponse(user)
        );
    }

    public static DailyNutritionTargetResponse toDailyNutritionTargetResponse(User user) {
        return new DailyNutritionTargetResponse(
                user.getDailyCalorieTarget(),
                user.getDailyProteinTarget(),
                user.getDailyCarbsTarget(),
                user.getDailyFatsTarget()
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

    private AuthSession createSession(long userId) {
        Instant expiresAt = Instant.now().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        return sessionStore.createSession(userId, expiresAt);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRecoveryAnswer(String answer) {
        return answer == null ? null : answer.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
