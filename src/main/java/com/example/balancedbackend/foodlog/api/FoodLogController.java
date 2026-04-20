package com.example.balancedbackend.foodlog.api;

import com.example.balancedbackend.foodlog.api.dto.FoodLogRequest;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogStatsResponse;
import com.example.balancedbackend.foodlog.api.dto.GenerationControlResponse;
import com.example.balancedbackend.foodlog.api.dto.GenerationStartRequest;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.service.FoodLogGeneratorService;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
@Validated
public class FoodLogController {

    private final FoodLogService foodLogService;
    private final FoodLogGeneratorService foodLogGeneratorService;

    public FoodLogController(FoodLogService foodLogService, FoodLogGeneratorService foodLogGeneratorService) {
        this.foodLogService = foodLogService;
        this.foodLogGeneratorService = foodLogGeneratorService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<FoodLogResponse>> getAll(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be >= 1") int size
    ) {
        return ResponseEntity.ok(foodLogService.getAll(user.id(), page, size));
    }

    @PostMapping
    public ResponseEntity<FoodLogResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FoodLogRequest request
    ) {
        FoodLogResponse created = foodLogService.create(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/generator/start")
    public ResponseEntity<GenerationControlResponse> startGenerator(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody(required = false) GenerationStartRequest request
    ) {
        Integer batchSize = request == null ? null : request.batchSize();
        Long intervalMs = request == null ? null : request.intervalMs();
        FoodLogGeneratorService.GeneratorStatus status = foodLogGeneratorService.start(user.id(), batchSize, intervalMs);

        return ResponseEntity.accepted().body(new GenerationControlResponse(
                status.running(),
                "Generator started",
                status.batchSize(),
                status.intervalMs()
        ));
    }

    @PostMapping("/generator/stop")
    public ResponseEntity<GenerationControlResponse> stopGenerator(@AuthenticationPrincipal AuthenticatedUser user) {
        FoodLogGeneratorService.GeneratorStatus status = foodLogGeneratorService.stop(user.id());

        return ResponseEntity.ok(new GenerationControlResponse(
                status.running(),
                "Generator stopped",
                status.batchSize(),
                status.intervalMs()
        ));
    }

    @GetMapping("/generator/status")
    public ResponseEntity<GenerationControlResponse> generatorStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        FoodLogGeneratorService.GeneratorStatus status = foodLogGeneratorService.status(user.id());

        return ResponseEntity.ok(new GenerationControlResponse(
                status.running(),
                "Generator status fetched",
                status.batchSize(),
                status.intervalMs()
        ));
    }

    @GetMapping("/day")
    public ResponseEntity<List<FoodLogResponse>> getByDay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Pattern(
                    regexp = "^\\d{4}-\\d{2}-\\d{2}$",
                    message = "date must be in YYYY-MM-DD format"
            ) String date
    ) {
        return ResponseEntity.ok(foodLogService.getByDay(user.id(), date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodLogResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(foodLogService.getById(user.id(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodLogResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id,
            @Valid @RequestBody FoodLogRequest request
    ) {
        return ResponseEntity.ok(foodLogService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id
    ) {
        foodLogService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<FoodLogStatsResponse> getStats(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(foodLogService.getStats(user.id()));
    }
}
