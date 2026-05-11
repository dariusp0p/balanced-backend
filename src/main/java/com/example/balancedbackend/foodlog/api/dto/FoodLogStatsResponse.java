package com.example.balancedbackend.foodlog.api.dto;

public record FoodLogStatsResponse(
        int totalLogs,
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFats,
        MacroDistributionResponse macroDistribution
) {
}