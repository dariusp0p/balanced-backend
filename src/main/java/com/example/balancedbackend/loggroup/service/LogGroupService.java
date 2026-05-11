package com.example.balancedbackend.loggroup.service;

import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.shared.exception.BadRequestException;
import com.example.balancedbackend.shared.exception.NotFoundException;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.store.FoodLogRepository;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.model.LogGroup;
import com.example.balancedbackend.loggroup.model.MealType;
import com.example.balancedbackend.loggroup.store.LogGroupRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class LogGroupService {

    private final LogGroupRepository logGroupRepository;
    private final FoodLogRepository foodLogRepository;
    private final AuditService auditService;

    public LogGroupService(
            LogGroupRepository logGroupRepository,
            FoodLogRepository foodLogRepository,
            AuditService auditService
    ) {
        this.logGroupRepository = logGroupRepository;
        this.foodLogRepository = foodLogRepository;
        this.auditService = auditService;
    }

    public LogGroupResponse create(long userId, LogGroupRequest request) {
        validateRequest(request);

        LogGroup group = LogGroup.builder()
                .userId(userId)
                .name(request.name().trim())
                .mealType(request.mealType() == null ? MealType.CUSTOM : request.mealType())
                .date(parseDate(request.date()))
                .computeFromFoodLogs(request.computeFromFoodLogs())
                .totalCalories(request.totalCalories())
                .totalProtein(request.totalProtein())
                .totalCarbs(request.totalCarbs())
                .totalFats(request.totalFats())
                .build();

        LogGroup saved = logGroupRepository.save(group);
        auditService.logAction(userId, "Created log group " + saved.getId() + " (" + saved.getName() + ")");
        return toResponse(saved);
    }

    public PagedResponse<LogGroupResponse> getAll(
            long userId,
            String date,
            MealType mealType,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"))
        );

        Page<LogGroup> result;

        if (date != null && !date.isBlank()) {
            result = logGroupRepository.findAllByUserIdAndDate(userId, parseDate(date), pageRequest);
        } else if (mealType != null) {
            result = logGroupRepository.findAllByUserIdAndMealType(userId, mealType, pageRequest);
        } else {
            result = logGroupRepository.findAllByUserId(userId, pageRequest);
        }

        List<LogGroupResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        String filters = date != null && !date.isBlank()
                ? " date " + parseDate(date)
                : mealType != null ? " mealType " + mealType : "";
        auditService.logAction(
                userId,
                "Viewed log groups page " + result.getNumber() + " size " + result.getSize() + filters
        );

        return new PagedResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public LogGroupResponse getById(long userId, long id) {
        LogGroup group = getOwnedGroup(userId, id);
        auditService.logAction(userId, "Viewed log group " + group.getId() + " (" + group.getName() + ")");
        return toResponse(group);
    }

    @Transactional
    public List<LogGroupResponse> ensureDefaultGroupsForEmptyDay(long userId, String date) {
        LocalDate selectedDate = parseDate(date);
        boolean hasGroups = logGroupRepository.existsByUserIdAndDate(userId, selectedDate);
        boolean hasLogs = foodLogRepository.existsByUserIdAndDate(userId, selectedDate);

        if (!hasGroups && !hasLogs) {
            logGroupRepository.saveAll(List.of(
                    createDefaultGroup(userId, selectedDate, "Breakfast", MealType.BREAKFAST),
                    createDefaultGroup(userId, selectedDate, "Lunch", MealType.LUNCH),
                    createDefaultGroup(userId, selectedDate, "Dinner", MealType.DINNER),
                    createDefaultGroup(userId, selectedDate, "Snacks", MealType.SNACK)
            ));
            auditService.logAction(userId, "Created default daily groups for " + selectedDate);
        }

        auditService.logAction(userId, "Viewed daily log groups for " + selectedDate);

        return logGroupRepository.findAllByUserIdAndDateOrderByIdAsc(userId, selectedDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LogGroupResponse update(long userId, long id, LogGroupRequest request) {
        validateRequest(request);

        LogGroup group = getOwnedGroup(userId, id);

        group.setName(request.name().trim());
        group.setMealType(request.mealType() == null ? MealType.CUSTOM : request.mealType());
        group.setDate(parseDate(request.date()));
        group.setComputeFromFoodLogs(request.computeFromFoodLogs());
        group.setTotalCalories(request.totalCalories());
        group.setTotalProtein(request.totalProtein());
        group.setTotalCarbs(request.totalCarbs());
        group.setTotalFats(request.totalFats());

        LogGroup saved = logGroupRepository.save(group);
        auditService.logAction(userId, "Updated log group " + saved.getId() + " (" + saved.getName() + ")");
        return toResponse(saved);
    }

    @Transactional
    public void delete(long userId, long id) {
        LogGroup group = getOwnedGroup(userId, id);

        foodLogRepository.deleteAllByUserIdAndGroupId(userId, group.getId());
        logGroupRepository.delete(group);
        auditService.logAction(userId, "Deleted log group " + id + " (" + group.getName() + ")");
    }

    private LogGroup getOwnedGroup(long userId, long id) {
        return logGroupRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Log group not found"));
    }

    private LogGroup createDefaultGroup(long userId, LocalDate date, String name, MealType mealType) {
        return LogGroup.builder()
                .userId(userId)
                .name(name)
                .mealType(mealType)
                .date(date)
                .computeFromFoodLogs(true)
                .totalCalories(0)
                .totalProtein(0)
                .totalCarbs(0)
                .totalFats(0)
                .build();
    }

    private LogGroupResponse toResponse(LogGroup group) {
        Totals totals = group.isComputeFromFoodLogs()
                ? computeTotalsFromLogs(group.getUserId(), group.getId())
                : new Totals(
                group.getTotalCalories(),
                group.getTotalProtein(),
                group.getTotalCarbs(),
                group.getTotalFats()
        );

        return new LogGroupResponse(
                group.getId(),
                group.getName(),
                group.getMealType(),
                group.getDate().toString(),
                group.isComputeFromFoodLogs(),
                round2(totals.calories()),
                round2(totals.protein()),
                round2(totals.carbs()),
                round2(totals.fats())
        );
    }

    private Totals computeTotalsFromLogs(long userId, long groupId) {
        List<FoodLog> logs = foodLogRepository.findAllByUserIdAndGroupId(userId, groupId);

        return new Totals(
                logs.stream().mapToDouble(FoodLog::getCalories).sum(),
                logs.stream().mapToDouble(FoodLog::getProtein).sum(),
                logs.stream().mapToDouble(FoodLog::getCarbs).sum(),
                logs.stream().mapToDouble(FoodLog::getFats).sum()
        );
    }

    private void validateRequest(LogGroupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Group name is required");
        }

        if (request.date() == null || request.date().isBlank()) {
            throw new BadRequestException("Date is required");
        }

        if (request.totalCalories() < 0 ||
                request.totalProtein() < 0 ||
                request.totalCarbs() < 0 ||
                request.totalFats() < 0) {
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

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Totals(
            double calories,
            double protein,
            double carbs,
            double fats
    ) {
    }
}
