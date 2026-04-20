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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogGroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/log-groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateListGetUpdateAndDeleteLogGroup() throws Exception {
        String token = signUpAndLogin("log-group-user@example.com");

        String createdBody = mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("Training Day", "2024-03-24", false, 2100, 150, 220, 65)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        mockMvc.perform(get("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Training Day"));

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(2100.0));

        mockMvc.perform(put("/api/log-groups/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("Rest Day", "2024-03-25", false, 1800, 130, 170, 55)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rest Day"));

        mockMvc.perform(delete("/api/log-groups/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldComputeTotalsFromFoodLogsWhenConfigured() throws Exception {
        String token = signUpAndLogin("log-group-compute@example.com");

        String groupBody = mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("Auto", "2024-03-24", true, 0, 0, 0, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.computeFromFoodLogs").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long groupId = objectMapper.readTree(groupBody).get("id").asLong();

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Breakfast", "2024-03-24", "08:15", groupId, 220, 18, 28, 4)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Lunch", "2024-03-25", "12:30", groupId, 500, 30, 55, 14)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/log-groups/{id}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(720.0))
                .andExpect(jsonPath("$.totalProtein").value(48.0));
    }

    @Test
    void shouldDeleteFoodLogsWhenGroupIsDeleted() throws Exception {
        String token = signUpAndLogin("log-group-cascade@example.com");

        String groupBody = mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("Auto", "2024-03-24", false, 0, 0, 0, 0)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = objectMapper.readTree(groupBody).get("id").asLong();

        String groupedLogBody = mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Grouped", "2024-03-24", "08:15", groupId, 220, 18, 28, 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logGroupId").value(groupId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String plainLogBody = mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Plain", "2024-03-24", "09:00", null, 180, 12, 20, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logGroupId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long groupedLogId = objectMapper.readTree(groupedLogBody).get("id").asLong();
        long plainLogId = objectMapper.readTree(plainLogBody).get("id").asLong();

        mockMvc.perform(delete("/api/log-groups/{id}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/food-logs/{id}", groupedLogId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/food-logs/{id}", plainLogId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plain"));
    }

    @Test
    void shouldValidateInputAndPagination() throws Exception {
        String token = signUpAndLogin("log-group-validation@example.com");

        mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("", "24-03-2024", false, -1, -1, -1, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists())
                .andExpect(jsonPath("$.fieldErrors.totalCalories").exists());

        mockMvc.perform(get("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page must be >= 0")));
    }

    @Test
    void shouldScopeDataPerUser() throws Exception {
        String tokenUserA = signUpAndLogin("log-group-alice@example.com");
        String tokenUserB = signUpAndLogin("log-group-bob@example.com");

        String createdBody = mockMvc.perform(post("/api/log-groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logGroupPayload("Private", "2024-03-24", false, 1000, 80, 90, 30)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    private String signUpAndLogin(String email) throws Exception {
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
        return json.get("token").asText();
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

