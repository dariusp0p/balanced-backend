package com.example.balancedbackend.auth.api.dto;

import java.time.Instant;

public record SignupResponse(
        String message,
        String token,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {
}
