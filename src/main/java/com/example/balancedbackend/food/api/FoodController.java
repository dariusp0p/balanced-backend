package com.example.balancedbackend.food.api;

import com.example.balancedbackend.food.api.dto.FoodRequest;
import com.example.balancedbackend.food.api.dto.FoodResponse;
import com.example.balancedbackend.food.service.FoodService;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping
    public FoodResponse create(
            @RequestParam long userId,
            @Valid @RequestBody FoodRequest request
    ) {
        return foodService.create(userId, request);
    }

    @GetMapping
    public PagedResponse<FoodResponse> getAll(
            @RequestParam long userId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return foodService.getAll(userId, query, page, size);
    }

    @GetMapping("/{id}")
    public FoodResponse getById(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        return foodService.getById(userId, id);
    }

    @PutMapping("/{id}")
    public FoodResponse update(
            @RequestParam long userId,
            @PathVariable long id,
            @Valid @RequestBody FoodRequest request
    ) {
        return foodService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        foodService.delete(userId, id);
    }
}