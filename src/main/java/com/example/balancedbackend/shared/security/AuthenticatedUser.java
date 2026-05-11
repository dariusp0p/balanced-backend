package com.example.balancedbackend.shared.security;

public record AuthenticatedUser(
        long id,
        String email,
        String name
) {
}

