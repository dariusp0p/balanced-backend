package com.example.balancedbackend.loggroup.api;

import com.example.balancedbackend.shared.exception.UnauthorizedException;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.model.MealType;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import com.example.balancedbackend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return logGroupService.create(requireUserId(authentication), request);
    }

    @GetMapping
    public PagedResponse<LogGroupResponse> getAll(
            Authentication authentication,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return logGroupService.getAll(requireUserId(authentication), date, mealType, page, size);
    }

    @GetMapping("/{id}")
    public LogGroupResponse getById(
            Authentication authentication,
            @PathVariable long id
    ) {
        return logGroupService.getById(requireUserId(authentication), id);
    }

    @PutMapping("/{id}")
    public LogGroupResponse update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return logGroupService.update(requireUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            Authentication authentication,
            @PathVariable long id
    ) {
        logGroupService.delete(requireUserId(authentication), id);
    }

    private long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }
}
