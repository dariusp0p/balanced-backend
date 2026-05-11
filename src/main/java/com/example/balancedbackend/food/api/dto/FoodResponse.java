package com.example.balancedbackend.food.api.dto;

import com.example.balancedbackend.food.model.FoodSource;

public record FoodResponse(
        Long id,
        String name,
        String brand,
        FoodSource source,
        String externalId,
        Double servingSize,
        String servingUnit,
        double caloriesPer100g,
        double proteinPer100g,
        double carbsPer100g,
        double fatsPer100g,
        Long createdByUserId
) {
}