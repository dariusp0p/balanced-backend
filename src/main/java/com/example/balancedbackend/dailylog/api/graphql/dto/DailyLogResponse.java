package com.example.balancedbackend.dailylog.api.graphql.dto;

import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;

import java.util.List;

public record DailyLogResponse(
        String date,
        List<LogGroupResponse> logGroups,
        List<FoodLogResponse> foodLogs,
        DailyNutritionTargetGraphqlResponse dailyNutritionTarget
) {
}
