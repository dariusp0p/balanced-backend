package com.example.balancedbackend.auth.store;

import com.example.balancedbackend.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query("""
            select r.name
            from Role r
            join UserRole ur on ur.roleId = r.id
            where ur.userId = :userId
            order by r.name
            """)
    List<String> findRoleNamesByUserId(@Param("userId") long userId);
}
