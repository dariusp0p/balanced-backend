package com.example.balancedbackend.auth.api.dto;

import java.util.List;

public record UserResponse(
        long id,
        String name,
        String email,
        boolean admin,
        List<String> roles,
        DailyNutritionTargetResponse dailyNutritionTarget
) {
}
