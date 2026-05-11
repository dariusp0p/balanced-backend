package com.example.balancedbackend.food.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "foods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Example: "Greek yogurt", "Chicken breast", "Oatmeal"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional. Useful for packaged foods.
     */
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodSource source;

    /**
     * ID from external source.
     * Example: Open Food Facts barcode or USDA/CIQUAL id.
     */
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "serving_size")
    private Double servingSize;

    @Column(name = "serving_unit")
    private String servingUnit;

    @Column(name = "calories_per_100g", nullable = false)
    private double caloriesPer100g;

    @Column(name = "protein_per_100g", nullable = false)
    private double proteinPer100g;

    @Column(name = "carbs_per_100g", nullable = false)
    private double carbsPer100g;

    @Column(name = "fats_per_100g", nullable = false)
    private double fatsPer100g;

    /**
     * Keep original API response if food comes from external DB.
     */
    @Column(name = "raw_source_json", columnDefinition = "text")
    private String rawSourceJson;

    /**
     * Null means global/imported food.
     * Non-null means food created by a specific user.
     */
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

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

        if (source == null) {
            source = FoodSource.CUSTOM;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}