package com.example.balancedbackend.config;

import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.InMemoryUserStore;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.store.InMemoryFoodLogStore;
import com.example.balancedbackend.loggroup.model.LogGroup;
import com.example.balancedbackend.loggroup.store.InMemoryLogGroupStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class SeedDataConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedDataConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner seedDataRunner(InMemoryUserStore userStore,
                                            InMemoryFoodLogStore foodLogStore,
                                            InMemoryLogGroupStore logGroupStore,
                                            PasswordEncoder passwordEncoder,
                                            @Value("${app.seed.demo-user.name:Demo User}") String seedName,
                                            @Value("${app.seed.demo-user.email:demo@balanced.local}") String seedEmail,
                                            @Value("${app.seed.demo-user.password:password123}") String seedPassword) {
        return args -> {
            if (userStore.findByEmail(seedEmail).isPresent()) {
                return;
            }

            User demoUser = userStore.createUser(seedName, seedEmail, passwordEncoder.encode(seedPassword));
            seedLogs(foodLogStore, demoUser.id());
            seedLogGroups(logGroupStore, demoUser.id());

            LOGGER.info("Seeded demo user {} with starter food logs", seedEmail);
        };
    }

    private void seedLogs(InMemoryFoodLogStore foodLogStore, long userId) {
        LocalDate baseDate = LocalDate.now();

        foodLogStore.create(new FoodLog(0, userId, "Greek Yogurt & Berries", baseDate, LocalTime.of(8, 15), 220, 18, 28, 4));
        foodLogStore.create(new FoodLog(0, userId, "Chicken Salad Bowl", baseDate, LocalTime.of(13, 0), 480, 42, 30, 18));

        foodLogStore.create(new FoodLog(0, userId, "Salmon with Rice", baseDate.minusDays(1), LocalTime.of(19, 30), 610, 38, 45, 24));
        foodLogStore.create(new FoodLog(0, userId, "Overnight Oats", baseDate.minusDays(1), LocalTime.of(7, 50), 350, 16, 52, 8));

        foodLogStore.create(new FoodLog(0, userId, "Turkey Wrap", baseDate.minusDays(2), LocalTime.of(12, 40), 430, 31, 36, 14));
        foodLogStore.create(new FoodLog(0, userId, "Tofu Stir Fry", baseDate.minusDays(2), LocalTime.of(19, 10), 520, 29, 58, 17));

        foodLogStore.create(new FoodLog(0, userId, "Protein Pancakes", baseDate.minusDays(3), LocalTime.of(8, 5), 410, 27, 44, 12));
        foodLogStore.create(new FoodLog(0, userId, "Quinoa Bowl", baseDate.minusDays(3), LocalTime.of(13, 25), 540, 24, 63, 19));

        foodLogStore.create(new FoodLog(0, userId, "Egg Sandwich", baseDate.minusDays(4), LocalTime.of(8, 20), 390, 22, 33, 16));
        foodLogStore.create(new FoodLog(0, userId, "Lean Beef Pasta", baseDate.minusDays(4), LocalTime.of(20, 0), 680, 44, 67, 23));

        foodLogStore.create(new FoodLog(0, userId, "Fruit Smoothie", baseDate.minusDays(5), LocalTime.of(9, 0), 280, 12, 47, 5));
        foodLogStore.create(new FoodLog(0, userId, "Shrimp Rice Bowl", baseDate.minusDays(5), LocalTime.of(18, 35), 560, 36, 61, 12));

        foodLogStore.create(new FoodLog(0, userId, "Cottage Cheese Toast", baseDate.minusDays(6), LocalTime.of(7, 45), 300, 21, 26, 11));
        foodLogStore.create(new FoodLog(0, userId, "Chicken Burrito", baseDate.minusDays(6), LocalTime.of(19, 20), 640, 41, 59, 20));
    }

    private void seedLogGroups(InMemoryLogGroupStore logGroupStore, long userId) {
        LocalDate baseDate = LocalDate.now();

        logGroupStore.create(new LogGroup(0, userId, "Today Plan", baseDate, true, 0, 0, 0, 0));
        logGroupStore.create(new LogGroup(0, userId, "Yesterday Plan", baseDate.minusDays(1), true, 0, 0, 0, 0));
        logGroupStore.create(new LogGroup(0, userId, "Meal Prep", baseDate.minusDays(2), false, 1600, 110, 170, 52));
    }
}
