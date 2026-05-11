package com.example.balancedbackend.loggroup.api;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogGroupControllerIntegrationTest {

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
    void fullCrudFlowShouldWork() throws Exception {
        signUp("Group User", "group@example.com");
        String token = tokenFromLogin("group@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lunch",
                                  "mealType":"LUNCH",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":500,
                                  "totalProtein":30,
                                  "totalCarbs":45,
                                  "totalFats":20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lunch"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lunch Updated",
                                  "mealType":"LUNCH",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":600,
                                  "totalProtein":40,
                                  "totalCarbs":50,
                                  "totalFats":25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lunch Updated"));

        mockMvc.perform(delete("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAndFiltersShouldWork() throws Exception {
        signUp("Group User", "group-filter@example.com");
        String token = tokenFromLogin("group-filter@example.com", "secret123");

        createGroup(token, "Breakfast", "BREAKFAST", "2026-05-11");
        createGroup(token, "Dinner", "DINNER", "2026-05-12");

        mockMvc.perform(get("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-05-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Breakfast"));

        mockMvc.perform(get("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .param("mealType", "DINNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dinner"));
    }

    @Test
    void computeFromFoodLogsShouldAggregateValues() throws Exception {
        signUp("Group User", "group-aggregate@example.com");
        String token = tokenFromLogin("group-aggregate@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Auto Group",
                                  "mealType":"LUNCH",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":true,
                                  "totalCalories":0,
                                  "totalProtein":0,
                                  "totalCarbs":0,
                                  "totalFats":0
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long groupId = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/food-logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "logGroupId": %d,
                                  "name":"Meal 1",
                                  "date":"2026-05-11",
                                  "time":"12:00",
                                  "quantity":1,
                                  "unit":"serving",
                                  "calories":200,
                                  "protein":20,
                                  "carbs":10,
                                  "fats":5
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/log-groups/{id}", groupId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(200.0))
                .andExpect(jsonPath("$.totalProtein").value(20.0));
    }

    @Test
    void shouldScopeDataPerUserAndValidate() throws Exception {
        signUp("Group User One", "group1@example.com");
        signUp("Group User Two", "group2@example.com");
        String token1 = tokenFromLogin("group1@example.com", "secret123");
        String token2 = tokenFromLogin("group2@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Private Group",
                                  "mealType":"LUNCH",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":100,
                                  "totalProtein":10,
                                  "totalCarbs":10,
                                  "totalFats":5
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"",
                                  "mealType":"LUNCH",
                                  "date":"bad",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":-1,
                                  "totalProtein":10,
                                  "totalCarbs":10,
                                  "totalFats":5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDefaultMealTypeAndRejectInvalidDateFilter() throws Exception {
        signUp("Group User", "group-default@example.com");
        String token = tokenFromLogin("group-default@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"No Meal Type",
                                  "date":"2026-05-11",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":100,
                                  "totalProtein":10,
                                  "totalCarbs":10,
                                  "totalFats":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealType").value("CUSTOM"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();
        mockMvc.perform(get("/api/log-groups/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealType").value("CUSTOM"));

        mockMvc.perform(get("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026/05/11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date must be in YYYY-MM-DD format"));
    }

    private void createGroup(String token, String name, String mealType, String date) throws Exception {
        mockMvc.perform(post("/api/log-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "mealType":"%s",
                                  "date":"%s",
                                  "computeFromFoodLogs":false,
                                  "totalCalories":100,
                                  "totalProtein":10,
                                  "totalCarbs":10,
                                  "totalFats":5
                                }
                                """.formatted(name, mealType, date)))
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
        return root.path("token").asText();
    }

    private void createRoleIfMissing(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) return;
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}
