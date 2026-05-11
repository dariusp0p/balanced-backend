package com.example.balancedbackend.food.api.dto;

import com.example.balancedbackend.food.model.FoodSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record FoodRequest(
        @NotBlank(message = "Name is required")
        String name,

        String brand,

        FoodSource source,

        String externalId,

        Double servingSize,

        String servingUnit,

        @PositiveOrZero(message = "Calories must be positive or zero")
        double caloriesPer100g,

        @PositiveOrZero(message = "Protein must be positive or zero")
        double proteinPer100g,

        @PositiveOrZero(message = "Carbs must be positive or zero")
        double carbsPer100g,

        @PositiveOrZero(message = "Fats must be positive or zero")
        double fatsPer100g,

        String rawSourceJson
) {
}