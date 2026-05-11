package com.example.balancedbackend.chat.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(
        @NotNull(message = "receiverId is required")
        Long receiverId,

        @NotBlank(message = "message is required")
        String content
) {
}
