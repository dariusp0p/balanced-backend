package com.example.balancedbackend.loggroup.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class LogGroupGraphqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReadLogGroupsAndFoodLogsViaGraphql() throws Exception {
        TestSession session = signUpAndLogin("graphql-user@example.com");

        String groupBody = mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .param("userId", String.valueOf(session.userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("GraphQL Day", "2024-03-24", true, 0, 0, 0, 0)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = objectMapper.readTree(groupBody).get("id").asLong();

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .param("userId", String.valueOf(session.userId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Breakfast", "2024-03-24", "08:15", groupId, 220, 18, 28, 4)))
                .andExpect(status().isOk());

        String groupsQuery = """
                {
                  \"query\":\"query { logGroups(page:0, size:10) { totalElements content { name date totalCalories } } }\"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupsQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logGroups.totalElements").value(1))
                .andExpect(jsonPath("$.data.logGroups.content[0].name").value("GraphQL Day"))
                .andExpect(jsonPath("$.data.logGroups.content[0].totalCalories").value(220.0));

        String logsQuery = """
                {
                  \"query\":\"query { foodLogsByDate(date: \\\"2024-03-24\\\") { name calories } }\"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logsQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foodLogsByDate[0].name").value("Breakfast"))
                .andExpect(jsonPath("$.data.foodLogsByDate[0].calories").value(220.0));
    }

    @Test
    void shouldReadDailyLogAndCreateDefaultGroupsForNewDay() throws Exception {
        TestSession session = signUpAndLogin("graphql-daily-user@example.com");

        String dailyQuery = """
                {
                  \"query\":\"query { dailyLog(date: \\\"2024-04-05\\\") { date foodLogs { name } logGroups { name mealType totalCalories } } }\"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dailyQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyLog.date").value("2024-04-05"))
                .andExpect(jsonPath("$.data.dailyLog.foodLogs.length()").value(0))
                .andExpect(jsonPath("$.data.dailyLog.logGroups.length()").value(4))
                .andExpect(jsonPath("$.data.dailyLog.logGroups[0].name").value("Breakfast"))
                .andExpect(jsonPath("$.data.dailyLog.logGroups[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.data.dailyLog.logGroups[1].name").value("Lunch"))
                .andExpect(jsonPath("$.data.dailyLog.logGroups[2].name").value("Dinner"))
                .andExpect(jsonPath("$.data.dailyLog.logGroups[3].name").value("Snacks"));

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dailyQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyLog.logGroups.length()").value(4));
    }

    @Test
    void shouldRejectUnauthenticatedGraphqlRequests() throws Exception {
        String query = """
                {
                  \"query\":\"query { logGroups { totalElements } }\"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isUnauthorized());
    }

    private TestSession signUpAndLogin(String email) throws Exception {
        String signupPayload = """
                {
                  "name":"Test User",
                  "email":"%s",
                  "password":"password123",
                  "confirmPassword":"password123"
                }
                """.formatted(email);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isCreated());

        String loginPayload = """
                {
                  "email":"%s",
                  "password":"password123"
                }
                """.formatted(email);

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        return new TestSession(json.get("token").asText(), json.get("user").get("id").asLong());
    }

    private record TestSession(String token, long userId) {
    }

    private String logGroupPayload(String name,
                                   String date,
                                   boolean computeFromFoodLogs,
                                   double totalCalories,
                                   double totalProtein,
                                   double totalCarbs,
                                   double totalFats) {
        return """
                {
                  "name":"%s",
                  "date":"%s",
                  "computeFromFoodLogs":%s,
                  "totalCalories":%s,
                  "totalProtein":%s,
                  "totalCarbs":%s,
                  "totalFats":%s
                }
                """.formatted(name, date, computeFromFoodLogs, totalCalories, totalProtein, totalCarbs, totalFats);
    }

    private String foodPayload(String name,
                               String date,
                               String time,
                               Long logGroupId,
                               double calories,
                               double protein,
                               double carbs,
                               double fats) {
        return """
                {
                  "name":"%s",
                  "date":"%s",
                  "time":"%s",
                  "logGroupId":%s,
                  "calories":%s,
                  "protein":%s,
                  "carbs":%s,
                  "fats":%s
                }
                """.formatted(name, date, time, logGroupId == null ? "null" : logGroupId, calories, protein, carbs, fats);
    }
}
