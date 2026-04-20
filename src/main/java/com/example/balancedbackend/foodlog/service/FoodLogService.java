package com.example.balancedbackend.foodlog.service;

import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogStatsResponse;
import com.example.balancedbackend.foodlog.api.dto.MacroDistributionResponse;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.store.InMemoryFoodLogStore;
import com.example.balancedbackend.loggroup.model.LogGroup;
import com.example.balancedbackend.loggroup.store.InMemoryLogGroupStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
public class FoodLogService {

    private final InMemoryFoodLogStore foodLogStore;
    private final InMemoryLogGroupStore logGroupStore;

    public FoodLogService(InMemoryFoodLogStore foodLogStore, InMemoryLogGroupStore logGroupStore) {
        this.foodLogStore = foodLogStore;
        this.logGroupStore = logGroupStore;
    }

    public FoodLogResponse create(long userId, FoodLogRequest request) {
        Long logGroupId = resolveOwnedLogGroupId(userId, request.logGroupId());
        FoodLog draft = new FoodLog(
                0,
                userId,
                logGroupId,
                request.name().trim(),
                parseDate(request.date()),
                parseTime(request.time()),
                request.calories(),
                request.protein(),
                request.carbs(),
                request.fats()
        );

        FoodLog saved = foodLogStore.create(draft);
        return toResponse(saved);
    }

    public PagedResponse<FoodLogResponse> getAll(long userId, int page, int size) {
        List<FoodLogResponse> sorted = foodLogStore.findAllByUserId(userId).stream()
                .sorted(Comparator
                        .comparing(FoodLog::date).reversed()
                        .thenComparing(FoodLog::time).reversed()
                        .thenComparing(FoodLog::id).reversed())
                .map(this::toResponse)
                .toList();

        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());

        List<FoodLogResponse> pageContent = sorted.subList(fromIndex, toIndex);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) sorted.size() / size);

        return new PagedResponse<>(pageContent, page, size, sorted.size(), totalPages);
    }

    public List<FoodLogResponse> getByDay(long userId, String date) {
        LocalDate selectedDate = parseDate(date);

        return foodLogStore.findAllByUserId(userId).stream()
                .filter(log -> log.date().equals(selectedDate))
                .sorted(Comparator
                        .comparing(FoodLog::time).reversed()
                        .thenComparing(FoodLog::id).reversed())
                .map(this::toResponse)
                .toList();
    }

    public FoodLogResponse getById(long userId, long id) {
        FoodLog foodLog = getOwnedFoodLog(userId, id);
        return toResponse(foodLog);
    }

    public FoodLogResponse update(long userId, long id, FoodLogRequest request) {
        FoodLog existing = getOwnedFoodLog(userId, id);
        Long logGroupId = resolveOwnedLogGroupId(userId, request.logGroupId());

        FoodLog updated = new FoodLog(
                existing.id(),
                existing.userId(),
                logGroupId,
                request.name().trim(),
                parseDate(request.date()),
                parseTime(request.time()),
                request.calories(),
                request.protein(),
                request.carbs(),
                request.fats()
        );

        return toResponse(foodLogStore.save(updated));
    }

    public void delete(long userId, long id) {
        FoodLog existing = getOwnedFoodLog(userId, id);
        foodLogStore.delete(existing.id());
    }

    public FoodLogStatsResponse getStats(long userId) {
        List<FoodLog> logs = foodLogStore.findAllByUserId(userId);

        double totalCalories = logs.stream().mapToDouble(FoodLog::calories).sum();
        double totalProtein = logs.stream().mapToDouble(FoodLog::protein).sum();
        double totalCarbs = logs.stream().mapToDouble(FoodLog::carbs).sum();
        double totalFats = logs.stream().mapToDouble(FoodLog::fats).sum();

        double macroSum = totalProtein + totalCarbs + totalFats;
        MacroDistributionResponse distribution = macroSum == 0
                ? new MacroDistributionResponse(0, 0, 0)
                : new MacroDistributionResponse(
                        round2(totalProtein * 100 / macroSum),
                        round2(totalCarbs * 100 / macroSum),
                        round2(totalFats * 100 / macroSum)
                );

        return new FoodLogStatsResponse(
                logs.size(),
                round2(totalCalories),
                round2(totalProtein),
                round2(totalCarbs),
                round2(totalFats),
                distribution
        );
    }

    private FoodLog getOwnedFoodLog(long userId, long id) {
        FoodLog foodLog = foodLogStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Food log not found"));

        if (foodLog.userId() != userId) {
            throw new NotFoundException("Food log not found");
        }

        return foodLog;
    }

    private FoodLogResponse toResponse(FoodLog foodLog) {
        return new FoodLogResponse(
                foodLog.id(),
                foodLog.logGroupId(),
                foodLog.name(),
                foodLog.date().toString(),
                foodLog.time().toString(),
                round2(foodLog.calories()),
                round2(foodLog.protein()),
                round2(foodLog.carbs()),
                round2(foodLog.fats())
        );
    }

    private Long resolveOwnedLogGroupId(long userId, Long logGroupId) {
        if (logGroupId == null) {
            return null;
        }

        LogGroup group = logGroupStore.findById(logGroupId)
                .orElseThrow(() -> new NotFoundException("Log group not found"));

        if (group.userId() != userId) {
            throw new NotFoundException("Log group not found");
        }

        return logGroupId;
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Date must be in YYYY-MM-DD format");
        }
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Time must be in HH:MM 24h format");
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
