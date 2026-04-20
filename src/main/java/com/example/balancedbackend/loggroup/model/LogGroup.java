package com.example.balancedbackend.loggroup.model;

import java.time.LocalDate;

public record LogGroup(
        long id,
        long userId,
        String name,
        LocalDate date,
        boolean computeFromFoodLogs,
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFats
) {
}

