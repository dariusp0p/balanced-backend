package com.example.balancedbackend.chat.api.dto;

import java.time.Instant;

public record ChatMessageResponse(
        String id,
        long senderId,
        String senderName,
        long receiverId,
        String content,
        Instant createdAt
) {
}
