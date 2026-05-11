package com.example.balancedbackend.auth.api;

import com.example.balancedbackend.auth.api.dto.AuthResponse;
import com.example.balancedbackend.auth.api.dto.LoginRequest;
import com.example.balancedbackend.auth.api.dto.SignupRequest;
import com.example.balancedbackend.auth.api.dto.SignupResponse;
import com.example.balancedbackend.auth.api.dto.UserResponse;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.auth.service.AuthService;
import com.example.balancedbackend.common.security.SecuritySupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final SecuritySupport securitySupport;
    private final RoleRepository roleRepository;

    public AuthController(
            AuthService authService,
            SecuritySupport securitySupport,
            RoleRepository roleRepository
    ) {
        this.authService = authService;
        this.securitySupport = securitySupport;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = securitySupport.requireUser(authentication);
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isAdmin(),
                roleRepository.findRoleNamesByUserId(user.getId())
        );
    }
}
