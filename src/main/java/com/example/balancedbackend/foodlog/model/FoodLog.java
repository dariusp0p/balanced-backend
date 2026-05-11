package com.example.balancedbackend.foodlog.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "food_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Temporary simple user ownership.
     * Later this can become @ManyToOne User.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Optional group/meal.
     */
    @Column(name = "group_id")
    private Long groupId;

    /**
     * Optional reference to reusable food.
     */
    @Column(name = "food_id")
    private Long foodId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private double quantity = 1.0;

    @Column(nullable = false)
    @Builder.Default
    private String unit = "serving";

    /**
     * Snapshot values for this exact log.
     */
    @Column(nullable = false)
    private double calories;

    @Column(nullable = false)
    private double protein;

    @Column(nullable = false)
    private double carbs;

    @Column(nullable = false)
    private double fats;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}