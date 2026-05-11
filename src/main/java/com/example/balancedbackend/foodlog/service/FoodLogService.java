package com.example.balancedbackend.foodlog.service;

import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.food.store.FoodRepository;
import com.example.balancedbackend.foodlog.api.dto.*;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.store.FoodLogRepository;
import com.example.balancedbackend.loggroup.store.LogGroupRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class FoodLogService {

    private final FoodLogRepository foodLogRepository;
    private final LogGroupRepository logGroupRepository;
    private final FoodRepository foodRepository;
    private final AuditService auditService;

    public FoodLogService(
            FoodLogRepository foodLogRepository,
            LogGroupRepository logGroupRepository,
            FoodRepository foodRepository,
            AuditService auditService
    ) {
        this.foodLogRepository = foodLogRepository;
        this.logGroupRepository = logGroupRepository;
        this.foodRepository = foodRepository;
        this.auditService = auditService;
    }

    public FoodLogResponse create(long userId, FoodLogRequest request) {
        validateRequest(request);

        Long groupId = resolveOwnedLogGroupId(userId, request.logGroupId());
        Long foodId = resolveVisibleFoodId(userId, request.foodId());

        FoodLog foodLog = FoodLog.builder()
                .userId(userId)
                .groupId(groupId)
                .foodId(foodId)
                .name(request.name().trim())
                .date(parseDate(request.date()))
                .time(parseTime(request.time()))
                .quantity(request.quantity() == null ? 1.0 : request.quantity())
                .unit(cleanUnit(request.unit()))
                .calories(request.calories())
                .protein(request.protein())
                .carbs(request.carbs())
                .fats(request.fats())
                .notes(cleanNullable(request.notes()))
                .build();

        FoodLog saved = foodLogRepository.save(foodLog);
        auditService.logAction(userId, "Created food log " + saved.getId() + " (" + saved.getName() + ")");
        observeIfDailyCaloriesAreSuspicious(userId, saved.getDate());
        return toResponse(saved);
    }

    public PagedResponse<FoodLogResponse> getAll(long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("date"),
                        Sort.Order.desc("time"),
                        Sort.Order.desc("id")
                )
        );

        Page<FoodLog> result = foodLogRepository.findAllByUserId(userId, pageRequest);

        List<FoodLogResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public List<FoodLogResponse> getByDay(long userId, String date) {
        LocalDate selectedDate = parseDate(date);

        return foodLogRepository
                .findAllByUserIdAndDateOrderByTimeDescIdDesc(userId, selectedDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FoodLogResponse getById(long userId, long id) {
        return toResponse(getOwnedFoodLog(userId, id));
    }

    public FoodLogResponse update(long userId, long id, FoodLogRequest request) {
        validateRequest(request);

        FoodLog existing = getOwnedFoodLog(userId, id);

        Long groupId = resolveOwnedLogGroupId(userId, request.logGroupId());
        Long foodId = resolveVisibleFoodId(userId, request.foodId());

        existing.setGroupId(groupId);
        existing.setFoodId(foodId);
        existing.setName(request.name().trim());
        existing.setDate(parseDate(request.date()));
        existing.setTime(parseTime(request.time()));
        existing.setQuantity(request.quantity() == null ? 1.0 : request.quantity());
        existing.setUnit(cleanUnit(request.unit()));
        existing.setCalories(request.calories());
        existing.setProtein(request.protein());
        existing.setCarbs(request.carbs());
        existing.setFats(request.fats());
        existing.setNotes(cleanNullable(request.notes()));

        FoodLog saved = foodLogRepository.save(existing);
        auditService.logAction(userId, "Updated food log " + saved.getId() + " (" + saved.getName() + ")");
        observeIfDailyCaloriesAreSuspicious(userId, saved.getDate());
        return toResponse(saved);
    }

    public void delete(long userId, long id) {
        FoodLog existing = getOwnedFoodLog(userId, id);
        foodLogRepository.delete(existing);
        auditService.logAction(userId, "Deleted food log " + id + " (" + existing.getName() + ")");
    }

    public FoodLogStatsResponse getStats(long userId) {
        List<FoodLog> logs = foodLogRepository.findAllByUserId(userId);

        double totalCalories = logs.stream().mapToDouble(FoodLog::getCalories).sum();
        double totalProtein = logs.stream().mapToDouble(FoodLog::getProtein).sum();
        double totalCarbs = logs.stream().mapToDouble(FoodLog::getCarbs).sum();
        double totalFats = logs.stream().mapToDouble(FoodLog::getFats).sum();

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
        return foodLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Food log not found"));
    }

    private void observeIfDailyCaloriesAreSuspicious(long userId, LocalDate date) {
        double dailyCalories = foodLogRepository.findAllByUserIdAndDateOrderByTimeDescIdDesc(userId, date)
                .stream()
                .mapToDouble(FoodLog::getCalories)
                .sum();

        if (dailyCalories > 10_000) {
            String reason = "Logged " + round2(dailyCalories) + " calories on " + date;
            auditService.observeUser(userId, reason);
            auditService.logAction(userId, "Marked observed: " + reason);
        }
    }

    private Long resolveOwnedLogGroupId(long userId, Long groupId) {
        if (groupId == null) {
            return null;
        }

        boolean exists = logGroupRepository.findByIdAndUserId(groupId, userId).isPresent();

        if (!exists) {
            throw new NotFoundException("Log group not found");
        }

        return groupId;
    }

    private Long resolveVisibleFoodId(long userId, Long foodId) {
        if (foodId == null) {
            return null;
        }

        boolean exists = foodRepository.findById(foodId)
                .filter(food -> food.getCreatedByUserId() == null || food.getCreatedByUserId().equals(userId))
                .isPresent();

        if (!exists) {
            throw new NotFoundException("Food not found");
        }

        return foodId;
    }

    private FoodLogResponse toResponse(FoodLog foodLog) {
        return new FoodLogResponse(
                foodLog.getId(),
                foodLog.getGroupId(),
                foodLog.getFoodId(),
                foodLog.getName(),
                foodLog.getDate().toString(),
                foodLog.getTime().toString(),
                round2(foodLog.getQuantity()),
                foodLog.getUnit(),
                round2(foodLog.getCalories()),
                round2(foodLog.getProtein()),
                round2(foodLog.getCarbs()),
                round2(foodLog.getFats()),
                foodLog.getNotes()
        );
    }

    private void validateRequest(FoodLogRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Food log name is required");
        }

        if (request.date() == null || request.date().isBlank()) {
            throw new BadRequestException("Date is required");
        }

        if (request.time() == null || request.time().isBlank()) {
            throw new BadRequestException("Time is required");
        }

        if (request.quantity() != null && request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        if (request.calories() < 0 || request.protein() < 0 || request.carbs() < 0 || request.fats() < 0) {
            throw new BadRequestException("Nutrition values cannot be negative");
        }
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

    private String cleanUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "serving";
        }

        return unit.trim();
    }

    private String cleanNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
