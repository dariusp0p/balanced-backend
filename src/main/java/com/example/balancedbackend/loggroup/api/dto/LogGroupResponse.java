package com.example.balancedbackend.loggroup.api.dto;

import com.example.balancedbackend.loggroup.model.MealType;

public record LogGroupResponse(
        Long id,
        String name,
        MealType mealType,
        String date,
        boolean computeFromFoodLogs,
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFats
) {
}