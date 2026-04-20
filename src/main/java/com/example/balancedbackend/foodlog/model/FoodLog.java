package com.example.balancedbackend.foodlog.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record FoodLog(
        long id,
        long userId,
        String name,
        LocalDate date,
        LocalTime time,
        double calories,
        double protein,
        double carbs,
        double fats
) {
}

