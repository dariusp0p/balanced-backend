package com.example.balancedbackend.loggroup.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LogGroupRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Date is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
        String date,

        boolean computeFromFoodLogs,

        @DecimalMin(value = "0", message = "Total calories must be >= 0")
        double totalCalories,

        @DecimalMin(value = "0", message = "Total protein must be >= 0")
        double totalProtein,

        @DecimalMin(value = "0", message = "Total carbs must be >= 0")
        double totalCarbs,

        @DecimalMin(value = "0", message = "Total fats must be >= 0")
        double totalFats
) {
}

