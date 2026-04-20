package com.example.balancedbackend.auth.model;

import java.time.Instant;

public record AuthSession(
        String token,
        long userId,
        Instant expiresAt
) {
}

