package com.example.balancedbackend.foodlog.store;

import com.example.balancedbackend.foodlog.model.FoodLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    Page<FoodLog> findAllByUserId(Long userId, Pageable pageable);

    List<FoodLog> findAllByUserId(Long userId);

    List<FoodLog> findAllByUserIdAndDateOrderByTimeDescIdDesc(Long userId, LocalDate date);

    List<FoodLog> findAllByUserIdAndGroupId(Long userId, Long groupId);

    Optional<FoodLog> findByIdAndUserId(Long id, Long userId);

    void deleteAllByUserIdAndGroupId(Long userId, Long groupId);
}