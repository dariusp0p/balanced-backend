package com.example.balancedbackend.loggroup.api;

import com.example.balancedbackend.loggroup.api.dto.LogGroupRequest;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.api.dto.PagedResponse;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import com.example.balancedbackend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

@RestController
@RequestMapping("/api/log-groups")
@Validated
public class LogGroupController {

    private final LogGroupService logGroupService;

    public LogGroupController(LogGroupService logGroupService) {
        this.logGroupService = logGroupService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LogGroupResponse>> getAll(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be >= 1") int size
    ) {
        return ResponseEntity.ok(logGroupService.getAll(user.id(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogGroupResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(logGroupService.getById(user.id(), id));
    }

    @PostMapping
    public ResponseEntity<LogGroupResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logGroupService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogGroupResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id,
            @Valid @RequestBody LogGroupRequest request
    ) {
        return ResponseEntity.ok(logGroupService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id
    ) {
        logGroupService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}

