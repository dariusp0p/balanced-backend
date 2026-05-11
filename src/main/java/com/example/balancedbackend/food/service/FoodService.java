package com.example.balancedbackend.food.service;

import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.NotFoundException;
import com.example.balancedbackend.food.api.dto.FoodRequest;
import com.example.balancedbackend.food.api.dto.FoodResponse;
import com.example.balancedbackend.food.model.Food;
import com.example.balancedbackend.food.model.FoodSource;
import com.example.balancedbackend.food.store.FoodRepository;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final AuditService auditService;

    public FoodService(FoodRepository foodRepository, AuditService auditService) {
        this.foodRepository = foodRepository;
        this.auditService = auditService;
    }

    public FoodResponse create(long userId, FoodRequest request) {
        validateFoodRequest(request);

        Food food = Food.builder()
                .name(request.name().trim())
                .brand(cleanNullable(request.brand()))
                .source(request.source() == null ? FoodSource.CUSTOM : request.source())
                .externalId(cleanNullable(request.externalId()))
                .servingSize(request.servingSize())
                .servingUnit(cleanNullable(request.servingUnit()))
                .caloriesPer100g(request.caloriesPer100g())
                .proteinPer100g(request.proteinPer100g())
                .carbsPer100g(request.carbsPer100g())
                .fatsPer100g(request.fatsPer100g())
                .rawSourceJson(request.rawSourceJson())
                .createdByUserId(userId)
                .build();

        Food saved = foodRepository.save(food);
        auditService.logAction(userId, "Created food " + saved.getId() + " (" + saved.getName() + ")");
        return toResponse(saved);
    }

    public PagedResponse<FoodResponse> getAll(long userId, String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );

        Page<Food> result;

        if (query == null || query.isBlank()) {
            result = foodRepository.findVisibleFoods(userId, pageRequest);
        } else {
            result = foodRepository.searchVisibleFoods(userId, query.trim(), pageRequest);
        }

        List<FoodResponse> content = result.getContent().stream()
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

    public FoodResponse getById(long userId, long id) {
        Food food = foodRepository.findById(id)
                .filter(f -> f.getCreatedByUserId() == null || f.getCreatedByUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Food not found"));

        return toResponse(food);
    }

    public FoodResponse update(long userId, long id, FoodRequest request) {
        validateFoodRequest(request);

        Food food = foodRepository.findByIdAndCreatedByUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Food not found"));

        food.setName(request.name().trim());
        food.setBrand(cleanNullable(request.brand()));
        food.setSource(request.source() == null ? FoodSource.CUSTOM : request.source());
        food.setExternalId(cleanNullable(request.externalId()));
        food.setServingSize(request.servingSize());
        food.setServingUnit(cleanNullable(request.servingUnit()));
        food.setCaloriesPer100g(request.caloriesPer100g());
        food.setProteinPer100g(request.proteinPer100g());
        food.setCarbsPer100g(request.carbsPer100g());
        food.setFatsPer100g(request.fatsPer100g());
        food.setRawSourceJson(request.rawSourceJson());

        Food saved = foodRepository.save(food);
        auditService.logAction(userId, "Updated food " + saved.getId() + " (" + saved.getName() + ")");
        return toResponse(saved);
    }

    public void delete(long userId, long id) {
        Food food = foodRepository.findByIdAndCreatedByUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Food not found"));

        foodRepository.delete(food);
        auditService.logAction(userId, "Deleted food " + id + " (" + food.getName() + ")");
    }

    private void validateFoodRequest(FoodRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Food name is required");
        }

        if (request.caloriesPer100g() < 0 ||
                request.proteinPer100g() < 0 ||
                request.carbsPer100g() < 0 ||
                request.fatsPer100g() < 0) {
            throw new BadRequestException("Nutrition values cannot be negative");
        }
    }

    private FoodResponse toResponse(Food food) {
        return new FoodResponse(
                food.getId(),
                food.getName(),
                food.getBrand(),
                food.getSource(),
                food.getExternalId(),
                food.getServingSize(),
                food.getServingUnit(),
                round2(food.getCaloriesPer100g()),
                round2(food.getProteinPer100g()),
                round2(food.getCarbsPer100g()),
                round2(food.getFatsPer100g()),
                food.getCreatedByUserId()
        );
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
