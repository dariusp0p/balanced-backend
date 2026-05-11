package com.example.balancedbackend.auth.api.dto;

public record DailyNutritionTargetResponse(
        double calories,
        double protein,
        double carbs,
        double fats
) {
}
