package com.example.balancedbackend.loggroup.api.graphql.dto;

import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;

import java.util.List;

public record FoodLogPageResponse(
        List<FoodLogResponse> content,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}

