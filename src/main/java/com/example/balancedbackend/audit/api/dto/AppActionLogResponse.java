package com.example.balancedbackend.audit.api.dto;

import java.time.Instant;

public record AppActionLogResponse(
        long id,
        long userId,
        String userName,
        String userEmail,
        String groupId,
        String actionInformation,
        Instant timestamp
) {
}
