package com.example.balancedbackend.dailylog.api.graphql;

import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DailyLogGraphqlIntegrationTest {

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
    void dailyLogShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"query($date:String!){ dailyLog(date:$date){ date } }",
                                  "variables":{"date":"2026-05-11"}
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dailyLogShouldReturnDefaultGroupsFoodLogsAndTargets() throws Exception {
        signUp("Graph User", "dailylog@example.com");
        String token = tokenFromLogin("dailylog@example.com", "secret123");

        // Prime with one log so foodLogs branch is populated.
        mockMvc.perform(post("/api/food-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Prime Log",
                                  "date":"2026-05-11",
                                  "time":"10:00",
                                  "quantity":1,
                                  "unit":"serving",
                                  "calories":250,
                                  "protein":20,
                                  "carbs":25,
                                  "fats":8
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"query($date:String!){ dailyLog(date:$date){ date logGroups { id name } foodLogs { id name calories } dailyNutritionTarget { calories protein carbs fats } } }",
                                  "variables":{"date":"2026-05-11"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyLog.date").value("2026-05-11"))
                .andExpect(jsonPath("$.data.dailyLog.foodLogs.length()").value(1))
                .andExpect(jsonPath("$.data.dailyLog.foodLogs[0].name").value("Prime Log"))
                .andExpect(jsonPath("$.data.dailyLog.dailyNutritionTarget.calories").value(2000.0))
                .andExpect(jsonPath("$.data.dailyLog.dailyNutritionTarget.protein").value(150.0))
                .andExpect(jsonPath("$.data.dailyLog.dailyNutritionTarget.carbs").value(250.0))
                .andExpect(jsonPath("$.data.dailyLog.dailyNutritionTarget.fats").value(70.0));
    }

    @Test
    void dailyLogShouldCreateDefaultGroupsForEmptyDay() throws Exception {
        signUp("Graph User", "dailylog-empty@example.com");
        String token = tokenFromLogin("dailylog-empty@example.com", "secret123");

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"query($date:String!){ dailyLog(date:$date){ date logGroups { name } foodLogs { id } } }",
                                  "variables":{"date":"2026-06-01"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyLog.logGroups.length()").value(4))
                .andExpect(jsonPath("$.data.dailyLog.foodLogs.length()").value(0));
    }

    private void signUp(String name, String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "email":"%s",
                                  "password":"secret123",
                                  "confirmPassword":"secret123",
                                  "recoveryQuestion":"What is your favorite food?",
                                  "recoveryAnswer":"Pizza"
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
