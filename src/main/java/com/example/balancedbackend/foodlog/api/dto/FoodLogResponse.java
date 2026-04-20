package com.example.balancedbackend.foodlog.api.dto;

public record FoodLogResponse(
        long id,
        Long logGroupId,
        String name,
        String date,
        String time,
        double calories,
        double protein,
        double carbs,
        double fats
) {

    public FoodLogResponse(
            long id,
            String name,
            String date,
            String time,
            double calories,
            double protein,
            double carbs,
            double fats
    ) {
        this(id, null, name, date, time, calories, protein, carbs, fats);
    }
}

