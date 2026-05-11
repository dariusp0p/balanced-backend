package com.example.balancedbackend.foodlog.api.dto;

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

