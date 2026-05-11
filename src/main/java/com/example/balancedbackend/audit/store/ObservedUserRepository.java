package com.example.balancedbackend.audit.store;

import com.example.balancedbackend.audit.model.ObservedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObservedUserRepository extends JpaRepository<ObservedUser, Long> {
    Optional<ObservedUser> findByUserId(Long userId);

    List<ObservedUser> findAllByActiveTrueOrderByObservedAtDesc();
}
