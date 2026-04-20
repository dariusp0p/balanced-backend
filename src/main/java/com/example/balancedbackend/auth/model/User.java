package com.example.balancedbackend.auth.model;

public record User(
        long id,
        String name,
        String email,
        String passwordHash
) {
}

