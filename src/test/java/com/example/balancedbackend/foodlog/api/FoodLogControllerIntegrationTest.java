package com.example.balancedbackend.foodlog.api;

import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FoodLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        createRoleIfMissing("ADMIN", "Full permissions");
        createRoleIfMissing("USER", "Restricted permissions");
    }

    @Test
    void fullCrudAndStatsFlowShouldWork() throws Exception {
        signUp("Log User", "foodlog@example.com");
        String token = tokenFromLogin("foodlog@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Breakfast Eggs",
                                  "date":"2026-05-11",
                                  "time":"08:00",
                                  "quantity":1,
                                  "unit":"serving",
                                  "calories":300,
                                  "protein":20,
                                  "carbs":10,
                                  "fats":18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Breakfast Eggs"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/food-logs/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/food-logs/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated Eggs",
                                  "date":"2026-05-11",
                                  "time":"08:10",
                                  "quantity":1.5,
                                  "unit":"serving",
                                  "calories":360,
                                  "protein":24,
                                  "carbs":12,
                                  "fats":20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Eggs"));

        mockMvc.perform(get("/api/food-logs/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(1))
                .andExpect(jsonPath("$.totalCalories").value(360.0));

        mockMvc.perform(delete("/api/food-logs/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/food-logs/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAndDayEndpointsShouldWork() throws Exception {
        signUp("Log User", "foodlog-list@example.com");
        String token = tokenFromLogin("foodlog-list@example.com", "secret123");

        createFoodLog(token, "Item A", "2026-05-10", "10:00", 100);
        createFoodLog(token, "Item B", "2026-05-10", "11:00", 200);
        createFoodLog(token, "Item C", "2026-05-11", "12:00", 300);

        mockMvc.perform(get("/api/food-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/food-logs/day")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldScopeDataPerUser() throws Exception {
        signUp("Log User 1", "foodlog1@example.com");
        signUp("Log User 2", "foodlog2@example.com");
        String token1 = tokenFromLogin("foodlog1@example.com", "secret123");
        String token2 = tokenFromLogin("foodlog2@example.com", "secret123");

        createFoodLog(token1, "Private Item", "2026-05-11", "09:00", 150);

        mockMvc.perform(get("/api/food-logs")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldValidateInputAndGeneratorLifecycle() throws Exception {
        signUp("Generator User", "generator@example.com");
        String token = tokenFromLogin("generator@example.com", "secret123");

        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"",
                                  "date":"bad-date",
                                  "time":"08:00",
                                  "calories":100,
                                  "protein":10,
                                  "carbs":10,
                                  "fats":5
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/food-logs/generator/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date":"2026-05-11",
                                  "batchSize":1,
                                  "intervalMs":1000
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.running").value(true));

        mockMvc.perform(get("/api/food-logs/generator/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true));

        mockMvc.perform(post("/api/food-logs/generator/stop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false));
    }

    @Test
    void shouldRejectInvalidDayDate() throws Exception {
        signUp("Day User", "day@example.com");
        String token = tokenFromLogin("day@example.com", "secret123");

        mockMvc.perform(get("/api/food-logs/day")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date must be in YYYY-MM-DD format"));
    }

    @Test
    void shouldApplyDefaultsAndRejectInvalidReferencesAndTime() throws Exception {
        signUp("Owner", "owner-foodlog@example.com");
        signUp("Other", "other-foodlog@example.com");
        String ownerToken = tokenFromLogin("owner-foodlog@example.com", "secret123");
        String otherToken = tokenFromLogin("other-foodlog@example.com", "secret123");

        MvcResult groupCreate = mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Owner Group",
                                  "mealType":"LUNCH",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":10,
                                  "totalProtein":1,
                                  "totalCarbs":1,
                                  "totalFats":1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long ownerGroupId = objectMapper.readTree(groupCreate.getResponse().getContentAsString()).path("id").asLong();

        MvcResult foodCreate = mockMvc.perform(post("/api/foods")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Owner Food",
                                  "caloriesPer100g":100,
                                  "proteinPer100g":10,
                                  "carbsPer100g":10,
                                  "fatsPer100g":10
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long ownerFoodId = objectMapper.readTree(foodCreate.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "logGroupId": %d,
                                  "name":"Bad Group Ref",
                                  "date":"2026-05-11",
                                  "time":"10:00",
                                  "calories":100,
                                  "protein":10,
                                  "carbs":10,
                                  "fats":10
                                }
                                """.formatted(ownerGroupId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Log group not found"));

        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodId": %d,
                                  "name":"Bad Food Ref",
                                  "date":"2026-05-11",
                                  "time":"10:00",
                                  "calories":100,
                                  "protein":10,
                                  "carbs":10,
                                  "fats":10
                                }
                                """.formatted(ownerFoodId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Food not found"));

        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Bad Time",
                                  "date":"2026-05-11",
                                  "time":"25:61",
                                  "calories":100,
                                  "protein":10,
                                  "carbs":10,
                                  "fats":10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Time must be in HH:MM 24h format"));

        MvcResult created = mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Defaulted",
                                  "date":"2026-05-11",
                                  "time":"10:00",
                                  "quantity": null,
                                  "unit":"   ",
                                  "notes":"   ",
                                  "calories":90,
                                  "protein":9,
                                  "carbs":9,
                                  "fats":9
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1.0))
                .andExpect(jsonPath("$.unit").value("serving"))
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andReturn();

        long createdId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        mockMvc.perform(get("/api/food-logs/{id}", createdId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit").value("serving"));
    }

    private void createFoodLog(String token, String name, String date, String time, int calories) throws Exception {
        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "date":"%s",
                                  "time":"%s",
                                  "quantity":1,
                                  "unit":"serving",
                                  "calories":%d,
                                  "protein":10,
                                  "carbs":10,
                                  "fats":5
                                }
                                """.formatted(name, date, time, calories)))
                .andExpect(status().isOk());
    }

    private void signUp(String name, String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "email":"%s",
                                  "password":"secret123",
                                  "confirmPassword":"secret123"
                                }
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
    }

    private String tokenFromLogin(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private void createRoleIfMissing(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) return;
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}
