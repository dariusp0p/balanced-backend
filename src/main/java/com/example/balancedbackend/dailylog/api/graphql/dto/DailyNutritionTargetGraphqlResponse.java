package com.example.balancedbackend.dailylog.api.graphql.dto;

public record DailyNutritionTargetGraphqlResponse(
        double calories,
        double protein,
        double carbs,
        double fats
) {
}
