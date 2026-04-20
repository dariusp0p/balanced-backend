package com.example.balancedbackend.loggroup.service;

import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.foodlog.store.InMemoryFoodLogStore;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.store.InMemoryLogGroupStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogGroupServiceTest {

    private LogGroupService logGroupService;
    private FoodLogService foodLogService;

    @BeforeEach
    void setUp() {
        InMemoryFoodLogStore foodLogStore = new InMemoryFoodLogStore();
        InMemoryLogGroupStore logGroupStore = new InMemoryLogGroupStore();
        foodLogService = new FoodLogService(foodLogStore, logGroupStore);
        logGroupService = new LogGroupService(logGroupStore, foodLogStore);
    }

    @Test
    void shouldCreateAndReadStoredTotalsGroup() {
        LogGroupRequest request = new LogGroupRequest("Cutting Day", "2024-03-24", false, 1800, 145, 180, 55);

        var created = logGroupService.create(1L, request);
        var fetched = logGroupService.getById(1L, created.id());

        assertThat(fetched.name()).isEqualTo("Cutting Day");
        assertThat(fetched.totalCalories()).isEqualTo(1800);
        assertThat(fetched.computeFromFoodLogs()).isFalse();
    }

    @Test
    void shouldComputeTotalsFromFoodLogsWhenEnabled() {
        var created = logGroupService.create(1L, new LogGroupRequest("Auto", "2024-03-24", true, 0, 0, 0, 0));
        foodLogService.create(1L, new FoodLogRequest("Breakfast", "2024-03-24", "08:15", created.id(), 220, 18, 28, 4));
        foodLogService.create(1L, new FoodLogRequest("Lunch", "2024-03-25", "12:30", created.id(), 500, 30, 55, 14));
        var fetched = logGroupService.getById(1L, created.id());

        assertThat(fetched.totalCalories()).isEqualTo(720);
        assertThat(fetched.totalProtein()).isEqualTo(48);
        assertThat(fetched.totalCarbs()).isEqualTo(83);
        assertThat(fetched.totalFats()).isEqualTo(18);
    }

    @Test
    void shouldDeleteGroup() {
        var created = logGroupService.create(1L, new LogGroupRequest("Delete", "2024-03-24", false, 1, 1, 1, 1));

        logGroupService.delete(1L, created.id());

        assertThatThrownBy(() -> logGroupService.getById(1L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldScopeGroupsPerUser() {
        var created = logGroupService.create(1L, new LogGroupRequest("Private", "2024-03-24", false, 10, 1, 1, 1));

        assertThatThrownBy(() -> logGroupService.getById(2L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldDeleteFoodLogsAssignedToDeletedGroup() {
        var group = logGroupService.create(1L, new LogGroupRequest("Delete with logs", "2024-03-24", false, 0, 0, 0, 0));
        var inGroup = foodLogService.create(1L,
                new FoodLogRequest("In group", "2024-03-24", "08:15", group.id(), 220, 18, 28, 4));
        var noGroup = foodLogService.create(1L,
                new FoodLogRequest("No group", "2024-03-24", "10:00", 180, 12, 20, 5));

        logGroupService.delete(1L, group.id());

        assertThatThrownBy(() -> foodLogService.getById(1L, inGroup.id()))
                .isInstanceOf(NotFoundException.class);
        assertThat(foodLogService.getById(1L, noGroup.id()).name()).isEqualTo("No group");
    }
}

