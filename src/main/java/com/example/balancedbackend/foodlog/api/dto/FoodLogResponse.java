package com.example.balancedbackend.foodlog.api.dto;

public record FoodLogResponse(
        Long id,
        Long logGroupId,
        Long foodId,
        String name,
        String date,
        String time,
        double quantity,
        String unit,
        double calories,
        double protein,
        double carbs,
        double fats,
        String notes
) {
}