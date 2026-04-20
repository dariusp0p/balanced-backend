package com.example.balancedbackend.security;

public record AuthenticatedUser(
        long id,
        String email,
        String name
) {
}

