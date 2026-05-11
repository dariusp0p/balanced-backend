package com.example.balancedbackend.foodlog.api;

import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogStatsResponse;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
public class FoodLogController {

    private final FoodLogService foodLogService;

    public FoodLogController(FoodLogService foodLogService) {
        this.foodLogService = foodLogService;
    }

    @PostMapping
    public FoodLogResponse create(
            @RequestParam long userId,
            @Valid @RequestBody FoodLogRequest request
    ) {
        return foodLogService.create(userId, request);
    }

    @GetMapping
    public PagedResponse<FoodLogResponse> getAll(
            @RequestParam long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return foodLogService.getAll(userId, page, size);
    }

    @GetMapping("/day")
    public List<FoodLogResponse> getByDay(
            @RequestParam long userId,
            @RequestParam String date
    ) {
        return foodLogService.getByDay(userId, date);
    }

    @GetMapping("/{id}")
    public FoodLogResponse getById(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        return foodLogService.getById(userId, id);
    }

    @PutMapping("/{id}")
    public FoodLogResponse update(
            @RequestParam long userId,
            @PathVariable long id,
            @Valid @RequestBody FoodLogRequest request
    ) {
        return foodLogService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        foodLogService.delete(userId, id);
    }

    @GetMapping("/stats")
    public FoodLogStatsResponse getStats(@RequestParam long userId) {
        return foodLogService.getStats(userId);
    }
}