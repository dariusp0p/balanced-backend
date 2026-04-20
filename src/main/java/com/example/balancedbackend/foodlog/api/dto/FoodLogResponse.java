package com.example.balancedbackend.foodlog.api.dto;

public record FoodLogResponse(
        long id,
        String name,
        String date,
        String time,
        double calories,
        double protein,
        double carbs,
        double fats
) {
}

