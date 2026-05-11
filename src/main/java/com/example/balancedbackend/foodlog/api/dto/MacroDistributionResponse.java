package com.example.balancedbackend.foodlog.api.dto;

public record MacroDistributionResponse(
        double proteinPercentage,
        double carbsPercentage,
        double fatsPercentage
) {
}