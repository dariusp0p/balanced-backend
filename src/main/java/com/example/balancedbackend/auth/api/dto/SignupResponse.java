package com.example.balancedbackend.auth.api.dto;

public record SignupResponse(
        String message,
        UserResponse user
) {
}

