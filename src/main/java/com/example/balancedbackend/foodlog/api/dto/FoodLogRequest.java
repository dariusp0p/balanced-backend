package com.example.balancedbackend.foodlog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record FoodLogRequest(
        Long logGroupId,

        Long foodId,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Date is required")
        String date,

        @NotBlank(message = "Time is required")
        String time,

        @Positive(message = "Quantity must be greater than zero")
        Double quantity,

        String unit,

        @PositiveOrZero(message = "Calories must be positive or zero")
        double calories,

        @PositiveOrZero(message = "Protein must be positive or zero")
        double protein,

        @PositiveOrZero(message = "Carbs must be positive or zero")
        double carbs,

        @PositiveOrZero(message = "Fats must be positive or zero")
        double fats,

        String notes
) {
}