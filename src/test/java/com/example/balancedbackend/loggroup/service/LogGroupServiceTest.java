package com.example.balancedbackend.loggroup.service;

import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:balanced-loggroup-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@Transactional
class LogGroupServiceTest {

    @Autowired
    private LogGroupService logGroupService;

    @Autowired
    private FoodLogService foodLogService;

    @Test
    void shouldCreateAndReadStoredTotalsGroup() {
        LogGroupRequest request = new LogGroupRequest("Cutting Day", null, "2024-03-24", false, 1800, 145, 180, 55);

        var created = logGroupService.create(1L, request);
        var fetched = logGroupService.getById(1L, created.id());

        assertThat(fetched.name()).isEqualTo("Cutting Day");
        assertThat(fetched.totalCalories()).isEqualTo(1800);
        assertThat(fetched.computeFromFoodLogs()).isFalse();
    }

    @Test
    void shouldComputeTotalsFromFoodLogsWhenEnabled() {
        var created = logGroupService.create(1L, new LogGroupRequest("Auto", null, "2024-03-24", true, 0, 0, 0, 0));
        foodLogService.create(1L, new FoodLogRequest(created.id(), null, "Breakfast", "2024-03-24", "08:15", null, null, 220, 18, 28, 4, null));
        foodLogService.create(1L, new FoodLogRequest(created.id(), null, "Lunch", "2024-03-25", "12:30", null, null, 500, 30, 55, 14, null));
        var fetched = logGroupService.getById(1L, created.id());

        assertThat(fetched.totalCalories()).isEqualTo(720);
        assertThat(fetched.totalProtein()).isEqualTo(48);
        assertThat(fetched.totalCarbs()).isEqualTo(83);
        assertThat(fetched.totalFats()).isEqualTo(18);
    }

    @Test
    void shouldDeleteGroup() {
        var created = logGroupService.create(1L, new LogGroupRequest("Delete", null, "2024-03-24", false, 1, 1, 1, 1));

        logGroupService.delete(1L, created.id());

        assertThatThrownBy(() -> logGroupService.getById(1L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldScopeGroupsPerUser() {
        var created = logGroupService.create(1L, new LogGroupRequest("Private", null, "2024-03-24", false, 10, 1, 1, 1));

        assertThatThrownBy(() -> logGroupService.getById(2L, created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldDeleteFoodLogsAssignedToDeletedGroup() {
        var group = logGroupService.create(1L, new LogGroupRequest("Delete with logs", null, "2024-03-24", false, 0, 0, 0, 0));
        var inGroup = foodLogService.create(1L,
                new FoodLogRequest(group.id(), null, "In group", "2024-03-24", "08:15", null, null, 220, 18, 28, 4, null));
        var noGroup = foodLogService.create(1L,
                new FoodLogRequest(null, null, "No group", "2024-03-24", "10:00", null, null, 180, 12, 20, 5, null));

        logGroupService.delete(1L, group.id());

        assertThatThrownBy(() -> foodLogService.getById(1L, inGroup.id()))
                .isInstanceOf(NotFoundException.class);
        assertThat(foodLogService.getById(1L, noGroup.id()).name()).isEqualTo("No group");
    }
}
