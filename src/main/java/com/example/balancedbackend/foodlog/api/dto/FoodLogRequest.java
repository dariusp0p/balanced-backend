package com.example.balancedbackend.foodlog.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FoodLogRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Date is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
        String date,

        @NotBlank(message = "Time is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Time must be in HH:MM 24h format")
        String time,

        @DecimalMin(value = "0", message = "Calories must be >= 0")
        double calories,

        @DecimalMin(value = "0", message = "Protein must be >= 0")
        double protein,

        @DecimalMin(value = "0", message = "Carbs must be >= 0")
        double carbs,

        @DecimalMin(value = "0", message = "Fats must be >= 0")
        double fats
) {
}

