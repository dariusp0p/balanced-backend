package com.example.balancedbackend.foodlog.service;

import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:balanced-foodlog-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@Transactional
class FoodLogServiceTest {

    @Autowired
    private FoodLogService foodLogService;

    @Autowired
    private LogGroupService logGroupService;

    @Test
    void shouldCreateAndReadFoodLog() {
        var request = new FoodLogRequest("Greek Yogurt", "2024-03-24", "08:15", 220, 18, 28, 4);
        var created = foodLogService.create(1L, request);

        var fetched = foodLogService.getById(1L, created.id());

        assertThat(fetched.name()).isEqualTo("Greek Yogurt");
        assertThat(fetched.calories()).isEqualTo(220);
    }

    @Test
    void shouldUpdateFoodLog() {
        var created = foodLogService.create(1L, new FoodLogRequest("Meal", "2024-03-24", "08:15", 220, 18, 28, 4));

        var updated = foodLogService.update(1L, created.id(),
                new FoodLogRequest("Updated Meal", "2024-03-25", "09:10", 300, 20, 30, 10));

        assertThat(updated.name()).isEqualTo("Updated Meal");
        assertThat(updated.date()).isEqualTo("2024-03-25");
    }

    @Test
    void shouldDeleteFoodLog() {
        var created = foodLogService.create(1L, new FoodLogRequest("Meal", "2024-03-24", "08:15", 220, 18, 28, 4));

        foodLogService.delete(1L, created.id());

        assertThatThrownBy(() -> foodLogService.getById(1L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldPaginateFoodLogs() {
        for (int i = 0; i < 7; i++) {
            foodLogService.create(1L, new FoodLogRequest("Meal " + i, "2024-03-24", "08:15", 100, 1, 2, 3));
        }

        var page0 = foodLogService.getAll(1L, 0, 5);
        var page1 = foodLogService.getAll(1L, 1, 5);

        assertThat(page0.content()).hasSize(5);
        assertThat(page1.content()).hasSize(2);
        assertThat(page0.totalElements()).isEqualTo(7);
        assertThat(page0.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldCalculateStats() {
        foodLogService.create(1L, new FoodLogRequest("Meal 1", "2024-03-24", "08:15", 200, 10, 20, 5));
        foodLogService.create(1L, new FoodLogRequest("Meal 2", "2024-03-24", "12:30", 300, 20, 30, 10));

        var stats = foodLogService.getStats(1L);

        assertThat(stats.totalEntries()).isEqualTo(2);
        assertThat(stats.totalCalories()).isEqualTo(500);
        assertThat(stats.totalProtein()).isEqualTo(30);
        assertThat(stats.totalCarbs()).isEqualTo(50);
        assertThat(stats.totalFats()).isEqualTo(15);
    }

    @Test
    void shouldHideLogsFromOtherUsers() {
        var created = foodLogService.create(1L, new FoodLogRequest("Meal", "2024-03-24", "08:15", 220, 18, 28, 4));

        assertThatThrownBy(() -> foodLogService.getById(2L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnOnlyLogsForSpecificDay() {
        foodLogService.create(1L, new FoodLogRequest("Meal 1", "2024-03-24", "08:15", 200, 10, 20, 5));
        foodLogService.create(1L, new FoodLogRequest("Meal 2", "2024-03-24", "12:30", 300, 20, 30, 10));
        foodLogService.create(1L, new FoodLogRequest("Meal 3", "2024-03-25", "09:45", 150, 8, 18, 3));

        var dailyLogs = foodLogService.getByDay(1L, "2024-03-24");

        assertThat(dailyLogs).hasSize(2);
        assertThat(dailyLogs).allMatch(log -> log.date().equals("2024-03-24"));
    }

    @Test
    void shouldAssignFoodLogToOwnedGroup() {
        var group = logGroupService.create(1L, new LogGroupRequest("Lunches", "2024-03-24", false, 0, 0, 0, 0));

        var created = foodLogService.create(1L,
                new FoodLogRequest("Meal", "2024-03-24", "08:15", group.id(), 220, 18, 28, 4));

        assertThat(created.logGroupId()).isEqualTo(group.id());
    }

    @Test
    void shouldRejectGroupFromAnotherUser() {
        var group = logGroupService.create(2L, new LogGroupRequest("Private", "2024-03-24", false, 0, 0, 0, 0));

        assertThatThrownBy(() -> foodLogService.create(1L,
                new FoodLogRequest("Meal", "2024-03-24", "08:15", group.id(), 220, 18, 28, 4)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Log group not found");
    }
}
