package com.example.balancedbackend.auth.api.dto;

public record UserResponse(
        long id,
        String name,
        String email
) {
}

