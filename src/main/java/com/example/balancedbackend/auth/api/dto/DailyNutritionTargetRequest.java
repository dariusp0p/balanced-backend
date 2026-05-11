package com.example.balancedbackend.auth.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record DailyNutritionTargetRequest(
        @PositiveOrZero(message = "calories must be zero or greater")
        double calories,

        @PositiveOrZero(message = "protein must be zero or greater")
        double protein,

        @PositiveOrZero(message = "carbs must be zero or greater")
        double carbs,

        @PositiveOrZero(message = "fats must be zero or greater")
        double fats
) {
}
