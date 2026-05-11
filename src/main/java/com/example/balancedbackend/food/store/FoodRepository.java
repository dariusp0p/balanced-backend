package com.example.balancedbackend.food.store;

import com.example.balancedbackend.food.model.Food;
import com.example.balancedbackend.food.model.FoodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    @Query("""
            select f from Food f
            where f.createdByUserId = :userId or f.createdByUserId is null
            """)
    Page<Food> findVisibleFoods(Long userId, Pageable pageable);

    @Query("""
            select f from Food f
            where lower(f.name) like lower(concat('%', :query, '%'))
              and (f.createdByUserId = :userId or f.createdByUserId is null)
            """)
    Page<Food> searchVisibleFoods(Long userId, String query, Pageable pageable);

    Page<Food> findAllBySource(FoodSource source, Pageable pageable);

    Optional<Food> findByIdAndCreatedByUserId(Long id, Long createdByUserId);

    Optional<Food> findBySourceAndExternalId(FoodSource source, String externalId);
}