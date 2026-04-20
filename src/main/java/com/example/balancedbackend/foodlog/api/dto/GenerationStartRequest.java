package com.example.balancedbackend.foodlog.api.dto;

import jakarta.validation.constraints.Min;

public record GenerationStartRequest(
        @Min(value = 1, message = "batchSize must be >= 1")
        Integer batchSize,

        @Min(value = 250, message = "intervalMs must be >= 250")
        Long intervalMs
) {
}

