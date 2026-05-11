package com.example.balancedbackend.loggroup.api.dto;

import com.example.balancedbackend.loggroup.model.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record LogGroupRequest(
        @NotBlank(message = "Name is required")
        String name,

        MealType mealType,

        @NotBlank(message = "Date is required")
        String date,

        boolean computeFromFoodLogs,

        @PositiveOrZero(message = "Calories must be positive or zero")
        double totalCalories,

        @PositiveOrZero(message = "Protein must be positive or zero")
        double totalProtein,

        @PositiveOrZero(message = "Carbs must be positive or zero")
        double totalCarbs,

        @PositiveOrZero(message = "Fats must be positive or zero")
        double totalFats
) {
}