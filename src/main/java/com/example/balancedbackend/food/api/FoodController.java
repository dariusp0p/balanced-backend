package com.example.balancedbackend.food.api;

import com.example.balancedbackend.common.exception.UnauthorizedException;
import com.example.balancedbackend.food.api.dto.FoodRequest;
import com.example.balancedbackend.food.api.dto.FoodResponse;
import com.example.balancedbackend.food.service.FoodService;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            @Valid @RequestBody FoodRequest request
    ) {
        return foodService.create(requireUserId(authentication), request);
    }

    @GetMapping
    public PagedResponse<FoodResponse> getAll(
            Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return foodService.getAll(requireUserId(authentication), query, page, size);
    }

    @GetMapping("/{id}")
    public FoodResponse getById(
            Authentication authentication,
            @PathVariable long id
    ) {
        return foodService.getById(requireUserId(authentication), id);
    }

    @PutMapping("/{id}")
    public FoodResponse update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody FoodRequest request
    ) {
        return foodService.update(requireUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            Authentication authentication,
            @PathVariable long id
    ) {
        foodService.delete(requireUserId(authentication), id);
    }

    private long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }
}
