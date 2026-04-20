package com.example.balancedbackend.foodlog.api.dto;

public record MacroDistributionResponse(
        double proteinPercent,
        double carbsPercent,
        double fatsPercent
) {
}

