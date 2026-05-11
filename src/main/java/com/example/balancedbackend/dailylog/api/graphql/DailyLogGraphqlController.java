package com.example.balancedbackend.dailylog.api.graphql;

import com.example.balancedbackend.dailylog.api.graphql.dto.DailyLogResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import com.example.balancedbackend.security.AuthenticatedUser;
import com.example.balancedbackend.common.exception.UnauthorizedException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DailyLogGraphqlController {

    private final LogGroupService logGroupService;
    private final FoodLogService foodLogService;

    public DailyLogGraphqlController(
            LogGroupService logGroupService,
            FoodLogService foodLogService
    ) {
        this.logGroupService = logGroupService;
        this.foodLogService = foodLogService;
    }

    @QueryMapping
    public DailyLogResponse dailyLog(
            @Argument String date,
            Authentication authentication
    ) {
        long userId = requireUserId(authentication);
        List<LogGroupResponse> groups = logGroupService.ensureDefaultGroupsForEmptyDay(userId, date);
        List<FoodLogResponse> logs = foodLogService.getByDay(userId, date);
        return new DailyLogResponse(date, groups, logs);
    }

    private long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user.id();
    }
}
