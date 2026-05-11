package com.example.balancedbackend.foodlog.api;

import com.example.balancedbackend.shared.exception.UnauthorizedException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogStatsResponse;
import com.example.balancedbackend.foodlog.api.dto.GenerationControlResponse;
import com.example.balancedbackend.foodlog.api.dto.GenerationStartRequest;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.service.FoodLogGeneratorService;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
public class FoodLogController {

    private final FoodLogService foodLogService;
    private final FoodLogGeneratorService foodLogGeneratorService;

    public FoodLogController(
            FoodLogService foodLogService,
            FoodLogGeneratorService foodLogGeneratorService
    ) {
        this.foodLogService = foodLogService;
        this.foodLogGeneratorService = foodLogGeneratorService;
    }

    @PostMapping
    public FoodLogResponse create(
            Authentication authentication,
            @Valid @RequestBody FoodLogRequest request
    ) {
        return foodLogService.create(requireUserId(authentication), request);
    }

    @GetMapping
    public PagedResponse<FoodLogResponse> getAll(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return foodLogService.getAll(requireUserId(authentication), page, size);
    }

    @GetMapping("/day")
    public List<FoodLogResponse> getByDay(
            Authentication authentication,
            @RequestParam String date
    ) {
        return foodLogService.getByDay(requireUserId(authentication), date);
    }

    @GetMapping("/{id}")
    public FoodLogResponse getById(
            Authentication authentication,
            @PathVariable long id
    ) {
        return foodLogService.getById(requireUserId(authentication), id);
    }

    @PutMapping("/{id}")
    public FoodLogResponse update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody FoodLogRequest request
    ) {
        return foodLogService.update(requireUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            Authentication authentication,
            @PathVariable long id
    ) {
        foodLogService.delete(requireUserId(authentication), id);
    }

    @GetMapping("/stats")
    public FoodLogStatsResponse getStats(Authentication authentication) {
        return foodLogService.getStats(requireUserId(authentication));
    }

    @PostMapping("/generator/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationControlResponse startGenerator(
            Authentication authentication,
            @Valid @RequestBody GenerationStartRequest request
    ) {
        FoodLogGeneratorService.GeneratorStatus status = foodLogGeneratorService.start(
                requireUserId(authentication),
                request.date(),
                request.batchSize(),
                request.intervalMs()
        );
        return toGeneratorResponse(status, "Generator started");
    }

    @PostMapping("/generator/stop")
    public GenerationControlResponse stopGenerator(Authentication authentication) {
        FoodLogGeneratorService.GeneratorStatus status =
                foodLogGeneratorService.stop(requireUserId(authentication));
        return toGeneratorResponse(status, "Generator stopped");
    }

    @GetMapping("/generator/status")
    public GenerationControlResponse generatorStatus(Authentication authentication) {
        FoodLogGeneratorService.GeneratorStatus status =
                foodLogGeneratorService.status(requireUserId(authentication));
        return toGeneratorResponse(status, status.running() ? "Generator running" : "Generator stopped");
    }

    private GenerationControlResponse toGeneratorResponse(
            FoodLogGeneratorService.GeneratorStatus status,
            String message
    ) {
        return new GenerationControlResponse(
                status.running(),
                message,
                status.batchSize(),
                status.intervalMs()
        );
    }

    private long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }
}
