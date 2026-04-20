package com.example.balancedbackend.foodlog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GenerationStartRequest(
        @NotBlank(message = "date is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be in YYYY-MM-DD format")
        String date,

        @Min(value = 1, message = "batchSize must be >= 1")
        Integer batchSize,

        @Min(value = 250, message = "intervalMs must be >= 250")
        Long intervalMs
) {
}
