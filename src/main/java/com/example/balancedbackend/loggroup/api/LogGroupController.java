package com.example.balancedbackend.loggroup.api;

import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.model.MealType;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/log-groups")
public class LogGroupController {

    private final LogGroupService logGroupService;

    public LogGroupController(LogGroupService logGroupService) {
        this.logGroupService = logGroupService;
    }

    @PostMapping
    public LogGroupResponse create(
            @RequestParam long userId,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return logGroupService.create(userId, request);
    }

    @GetMapping
    public PagedResponse<LogGroupResponse> getAll(
            @RequestParam long userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return logGroupService.getAll(userId, date, mealType, page, size);
    }

    @GetMapping("/{id}")
    public LogGroupResponse getById(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        return logGroupService.getById(userId, id);
    }

    @PutMapping("/{id}")
    public LogGroupResponse update(
            @RequestParam long userId,
            @PathVariable long id,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return logGroupService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestParam long userId,
            @PathVariable long id
    ) {
        logGroupService.delete(userId, id);
    }
}