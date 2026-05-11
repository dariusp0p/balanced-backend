package com.example.balancedbackend.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "observed_users")
@Getter
@Setter
public class ObservedUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        if (observedAt == null) {
            observedAt = Instant.now();
        }
    }
}
