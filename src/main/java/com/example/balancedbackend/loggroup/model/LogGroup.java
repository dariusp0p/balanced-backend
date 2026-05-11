package com.example.balancedbackend.loggroup.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "food_log_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Temporary simple user ownership.
     * Later this can become @ManyToOne User.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * If true, totals are calculated from child food logs.
     * If false, totals are manually stored here.
     */
    @Column(name = "compute_from_food_logs", nullable = false)
    private boolean computeFromFoodLogs;

    @Column(name = "total_calories", nullable = false)
    private double totalCalories;

    @Column(name = "total_protein", nullable = false)
    private double totalProtein;

    @Column(name = "total_carbs", nullable = false)
    private double totalCarbs;

    @Column(name = "total_fats", nullable = false)
    private double totalFats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (mealType == null) {
            mealType = MealType.CUSTOM;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}