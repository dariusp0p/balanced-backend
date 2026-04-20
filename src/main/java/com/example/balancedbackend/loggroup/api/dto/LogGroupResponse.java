package com.example.balancedbackend.loggroup.api.dto;

public record LogGroupResponse(
        long id,
        String name,
        String date,
        boolean computeFromFoodLogs,
        double totalCalories,
        double totalProtein,
        double totalCarbs,
        double totalFats
) {
}

