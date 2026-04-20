package com.example.balancedbackend.loggroup.service;

import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.store.InMemoryFoodLogStore;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.api.dto.PagedResponse;
import com.example.balancedbackend.loggroup.model.LogGroup;
import com.example.balancedbackend.loggroup.store.InMemoryLogGroupStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
public class LogGroupService {

    private final InMemoryLogGroupStore logGroupStore;
    private final InMemoryFoodLogStore foodLogStore;

    public LogGroupService(InMemoryLogGroupStore logGroupStore, InMemoryFoodLogStore foodLogStore) {
        this.logGroupStore = logGroupStore;
        this.foodLogStore = foodLogStore;
    }

    public LogGroupResponse create(long userId, LogGroupRequest request) {
        LogGroup draft = new LogGroup(
                0,
                userId,
                request.name().trim(),
                parseDate(request.date()),
                request.computeFromFoodLogs(),
                request.totalCalories(),
                request.totalProtein(),
                request.totalCarbs(),
                request.totalFats()
        );

        return toResponse(logGroupStore.create(draft));
    }

    public PagedResponse<LogGroupResponse> getAll(long userId, int page, int size) {
        List<LogGroupResponse> sorted = logGroupStore.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(LogGroup::date).reversed().thenComparing(LogGroup::id).reversed())
                .map(this::toResponse)
                .toList();

        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());

        List<LogGroupResponse> pageContent = sorted.subList(fromIndex, toIndex);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) sorted.size() / size);

        return new PagedResponse<>(pageContent, page, size, sorted.size(), totalPages);
    }

    public LogGroupResponse getById(long userId, long id) {
        return toResponse(getOwnedGroup(userId, id));
    }

    public LogGroupResponse update(long userId, long id, LogGroupRequest request) {
        LogGroup existing = getOwnedGroup(userId, id);

        LogGroup updated = new LogGroup(
                existing.id(),
                existing.userId(),
                request.name().trim(),
                parseDate(request.date()),
                request.computeFromFoodLogs(),
                request.totalCalories(),
                request.totalProtein(),
                request.totalCarbs(),
                request.totalFats()
        );

        return toResponse(logGroupStore.save(updated));
    }

    public void delete(long userId, long id) {
        LogGroup existing = getOwnedGroup(userId, id);
        foodLogStore.deleteAllByUserIdAndLogGroupId(userId, existing.id());
        logGroupStore.delete(existing.id());
    }

    private LogGroup getOwnedGroup(long userId, long id) {
        LogGroup group = logGroupStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Log group not found"));

        if (group.userId() != userId) {
            throw new NotFoundException("Log group not found");
        }

        return group;
    }

    private LogGroupResponse toResponse(LogGroup group) {
        Totals totals = group.computeFromFoodLogs()
                ? computeTotalsFromLogs(group.userId(), group.id())
                : new Totals(group.totalCalories(), group.totalProtein(), group.totalCarbs(), group.totalFats());

        return new LogGroupResponse(
                group.id(),
                group.name(),
                group.date().toString(),
                group.computeFromFoodLogs(),
                round2(totals.calories()),
                round2(totals.protein()),
                round2(totals.carbs()),
                round2(totals.fats())
        );
    }

    private Totals computeTotalsFromLogs(long userId, long groupId) {
        List<FoodLog> logs = foodLogStore.findAllByUserId(userId).stream()
                .filter(log -> Long.valueOf(groupId).equals(log.logGroupId()))
                .toList();

        return new Totals(
                logs.stream().mapToDouble(FoodLog::calories).sum(),
                logs.stream().mapToDouble(FoodLog::protein).sum(),
                logs.stream().mapToDouble(FoodLog::carbs).sum(),
                logs.stream().mapToDouble(FoodLog::fats).sum()
        );
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Date must be in YYYY-MM-DD format");
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Totals(double calories, double protein, double carbs, double fats) {
    }
}

