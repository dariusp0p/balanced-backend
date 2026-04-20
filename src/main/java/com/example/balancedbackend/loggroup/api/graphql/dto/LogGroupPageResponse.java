package com.example.balancedbackend.loggroup.api.graphql.dto;

import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;

import java.util.List;

public record LogGroupPageResponse(
        List<LogGroupResponse> content,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}

