package com.example.balancedbackend.foodlog.service.dto;

import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogStatsResponse;

import java.time.Instant;
import java.util.List;

public record FoodLogBatchEvent(
        long userId,
        int generatedCount,
        Instant generatedAt,
        List<FoodLogResponse> items,
        FoodLogStatsResponse stats
) {
}

