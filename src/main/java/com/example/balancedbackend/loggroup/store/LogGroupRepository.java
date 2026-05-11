package com.example.balancedbackend.loggroup.store;

import com.example.balancedbackend.loggroup.model.LogGroup;
import com.example.balancedbackend.loggroup.model.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LogGroupRepository extends JpaRepository<LogGroup, Long> {

    Page<LogGroup> findAllByUserId(Long userId, Pageable pageable);

    Page<LogGroup> findAllByUserIdAndDate(Long userId, LocalDate date, Pageable pageable);

    List<LogGroup> findAllByUserIdAndDateOrderByIdAsc(Long userId, LocalDate date);

    Page<LogGroup> findAllByUserIdAndMealType(Long userId, MealType mealType, Pageable pageable);

    Optional<LogGroup> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);
}
