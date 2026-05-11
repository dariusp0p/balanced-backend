package com.example.balancedbackend.dailylog.api.graphql;

import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.shared.security.SecuritySupport;
import com.example.balancedbackend.dailylog.api.graphql.dto.DailyLogResponse;
import com.example.balancedbackend.dailylog.api.graphql.dto.DailyNutritionTargetGraphqlResponse;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.service.FoodLogService;
import com.example.balancedbackend.loggroup.api.dto.LogGroupResponse;
import com.example.balancedbackend.loggroup.service.LogGroupService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DailyLogGraphqlController {

    private final LogGroupService logGroupService;
    private final FoodLogService foodLogService;
    private final SecuritySupport securitySupport;

    public DailyLogGraphqlController(
            LogGroupService logGroupService,
            FoodLogService foodLogService,
            SecuritySupport securitySupport
    ) {
        this.logGroupService = logGroupService;
        this.foodLogService = foodLogService;
        this.securitySupport = securitySupport;
    }

    @QueryMapping
    public DailyLogResponse dailyLog(
            @Argument String date,
            Authentication authentication
    ) {
        User user = securitySupport.requireUser(authentication);
        long userId = user.getId();
        List<LogGroupResponse> groups = logGroupService.ensureDefaultGroupsForEmptyDay(userId, date);
        List<FoodLogResponse> logs = foodLogService.getByDay(userId, date);
        return new DailyLogResponse(
                date,
                groups,
                logs,
                new DailyNutritionTargetGraphqlResponse(
                        user.getDailyCalorieTarget(),
                        user.getDailyProteinTarget(),
                        user.getDailyCarbsTarget(),
                        user.getDailyFatsTarget()
                )
        );
    }
}
