package com.example.balancedbackend.auth.store;

import com.example.balancedbackend.auth.model.UserRole;
import com.example.balancedbackend.auth.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
