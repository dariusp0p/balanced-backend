package com.example.balancedbackend.loggroup.api.graphql;

import com.example.balancedbackend.common.exception.UnauthorizedException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.api.dto.PagedResponse;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.api.graphql.dto.FoodLogPageResponse;
import com.example.balancedbackend.loggroup.api.graphql.dto.LogGroupPageResponse;
import com.example.balancedbackend.loggroup.model.MealType;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import com.example.balancedbackend.security.AuthenticatedUser;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class LogGroupGraphqlController {

    private final LogGroupService logGroupService;
    private final FoodLogService foodLogService;

    public LogGroupGraphqlController(
            LogGroupService logGroupService,
            FoodLogService foodLogService
    ) {
        this.logGroupService = logGroupService;
        this.foodLogService = foodLogService;
    }

    @QueryMapping
    public LogGroupPageResponse logGroups(
            @Argument String date,
            @Argument MealType mealType,
            @Argument Integer page,
            @Argument Integer size,
            Authentication authentication
    ) {
        long userId = requireUserId(authentication);

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 10 : size;

        PagedResponse<LogGroupResponse> result =
                logGroupService.getAll(userId, date, mealType, resolvedPage, resolvedSize);

        return new LogGroupPageResponse(
                result.content(),
                result.page(),
                result.size(),
                Math.toIntExact(result.totalElements()),
                result.totalPages()
        );
    }

    @QueryMapping
    public LogGroupResponse logGroup(
            @Argument Long id,
            Authentication authentication
    ) {
        long userId = requireUserId(authentication);
        return logGroupService.getById(userId, id);
    }

    @QueryMapping
    public FoodLogPageResponse foodLogs(
            @Argument Integer page,
            @Argument Integer size,
            Authentication authentication
    ) {
        long userId = requireUserId(authentication);

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 10 : size;

        PagedResponse<FoodLogResponse> result =
                foodLogService.getAll(userId, resolvedPage, resolvedSize);

        return new FoodLogPageResponse(
                result.content(),
                result.page(),
                result.size(),
                Math.toIntExact(result.totalElements()),
                result.totalPages()
        );
    }

    @QueryMapping
    public List<FoodLogResponse> foodLogsByDate(
            @Argument String date,
            Authentication authentication
    ) {
        long userId = requireUserId(authentication);
        return foodLogService.getByDay(userId, date);
    }

    private long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }
}
