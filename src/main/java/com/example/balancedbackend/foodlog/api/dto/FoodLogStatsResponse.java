package com.example.balancedbackend.foodlog.api.dto;

public record FoodLogStatsResponse(
        long totalEntries,
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFats,
        MacroDistributionResponse macroDistribution
) {
}

