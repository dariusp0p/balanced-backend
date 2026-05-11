package com.example.balancedbackend.audit.api.dto;

import java.time.Instant;

public record ObservedUserResponse(
        long id,
        long userId,
        String userName,
        String userEmail,
        String reason,
        Instant observedAt
) {
}
