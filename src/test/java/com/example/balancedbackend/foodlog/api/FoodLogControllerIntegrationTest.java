package com.example.balancedbackend.foodlog.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoodLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/food-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateListGetUpdateDeleteAndReturnStats() throws Exception {
        String token = signUpAndLogin("user1@example.com");

        String createdBody = mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Breakfast", "2024-03-24", "08:15", 220, 18, 28, 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdJson = objectMapper.readTree(createdBody);
        long id = createdJson.get("id").asLong();

        mockMvc.perform(get("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Breakfast"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/food-logs/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Breakfast"));

        mockMvc.perform(put("/api/food-logs/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Lunch", "2024-03-24", "12:30", 500, 30, 55, 14)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lunch"));

        mockMvc.perform(get("/api/food-logs/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(500.0))
                .andExpect(jsonPath("$.macroDistribution.proteinPercent").exists());

        mockMvc.perform(delete("/api/food-logs/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/food-logs/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateFoodLogInput() throws Exception {
        String token = signUpAndLogin("user2@example.com");

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("", "bad-date", "25:99", -1, -1, -1, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists())
                .andExpect(jsonPath("$.fieldErrors.time").exists())
                .andExpect(jsonPath("$.fieldErrors.calories").exists());
    }

    @Test
    void shouldScopeDataPerUser() throws Exception {
        String tokenUserA = signUpAndLogin("alice@example.com");
        String tokenUserB = signUpAndLogin("bob@example.com");

        String createdBody = mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Private Meal", "2024-03-24", "08:15", 200, 10, 20, 5)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        mockMvc.perform(get("/api/food-logs/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());

        String listBody = mockMvc.perform(get("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(listBody).get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(0);
    }

    @Test
    void shouldSupportPagination() throws Exception {
        String token = signUpAndLogin("paging@example.com");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/food-logs")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(foodPayload("Meal " + i, "2024-03-24", "08:1" + i, 100 + i, 10, 20, 5)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void shouldRejectInvalidPaginationParams() throws Exception {
        String token = signUpAndLogin("pagination-errors@example.com");

        mockMvc.perform(get("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page must be >= 0")));
    }

    @Test
    void shouldGetLogsByDay() throws Exception {
        String token = signUpAndLogin("day-filter@example.com");

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Breakfast", "2024-03-24", "08:15", 220, 18, 28, 4)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodPayload("Dinner", "2024-03-25", "19:30", 500, 35, 40, 20)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/food-logs/day")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("date", "2024-03-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Breakfast"));
    }

    @Test
    void shouldRejectInvalidDateForDayEndpoint() throws Exception {
        String token = signUpAndLogin("day-filter-invalid@example.com");

        mockMvc.perform(get("/api/food-logs/day")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("date", "24-03-2024"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("date must be in YYYY-MM-DD format")));
    }

    @Test
    void seededUserShouldBeAbleToLoginAndFetchDayLogs() throws Exception {
        String token = login("demo@balanced.local", "password123");

        mockMvc.perform(get("/api/food-logs/day")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("date", java.time.LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    void shouldStartAndStopGeneratorLoop() throws Exception {
        String token = signUpAndLogin("generator-user@example.com");
        String targetDate = "2024-03-21";

        mockMvc.perform(post("/api/food-logs/generator/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + targetDate + "\",\"batchSize\":2,\"intervalMs\":500}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.running").value(true));

        Thread.sleep(700);

        mockMvc.perform(get("/api/food-logs/day")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("date", targetDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());

        mockMvc.perform(get("/api/food-logs/generator/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true));

        mockMvc.perform(post("/api/food-logs/generator/stop")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false));
    }

    private String login(String email, String password) throws Exception {
        String loginPayload = """
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, password);

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("token").asText();
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

        return objectMapper.readTree(responseBody).get("token").asText();
    }

    private String foodPayload(String name,
                               String date,
                               String time,
                               double calories,
                               double protein,
                               double carbs,
                               double fats) {
        return """
                {
                  "name":"%s",
                  "date":"%s",
                  "time":"%s",
                  "calories":%s,
                  "protein":%s,
                  "carbs":%s,
                  "fats":%s
                }
                """.formatted(name, date, time, calories, protein, carbs, fats);
    }
}
