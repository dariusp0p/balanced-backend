package com.example.balancedbackend.foodlog.api.dto;

public record GenerationControlResponse(
        boolean running,
        String message,
        int batchSize,
        long intervalMs
) {
}

