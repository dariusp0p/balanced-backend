package com.example.balancedbackend.auth.model;

import java.io.Serializable;

public record UserRoleId(
        Long userId,
        Long roleId
) implements Serializable {
}
