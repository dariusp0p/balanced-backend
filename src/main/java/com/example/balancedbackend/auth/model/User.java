package com.example.balancedbackend.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private boolean admin = false;

    @Column(name = "daily_calorie_target", nullable = false)
    @Builder.Default
    private double dailyCalorieTarget = 2000;

    @Column(name = "daily_protein_target", nullable = false)
    @Builder.Default
    private double dailyProteinTarget = 150;

    @Column(name = "daily_carbs_target", nullable = false)
    @Builder.Default
    private double dailyCarbsTarget = 250;

    @Column(name = "daily_fats_target", nullable = false)
    @Builder.Default
    private double dailyFatsTarget = 70;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(Long id, String name, String email, String passwordHash) {
        this.id = id;
        this.firstName = normalizeName(name);
        this.lastName = null;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.admin = false;
        this.dailyCalorieTarget = 2000;
        this.dailyProteinTarget = 150;
        this.dailyCarbsTarget = 250;
        this.dailyFatsTarget = 70;
        this.createdAt = Instant.now();
    }

    public User(String name, String email, String passwordHash) {
        this(null, name, email, passwordHash);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        email = normalizeEmail(email);
        firstName = normalizeName(firstName);
    }

    @Transient
    public String getName() {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return getName();
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
