package com.example.balancedbackend.food.api;

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
class FoodControllerIntegrationTest {

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
        signUp("Food User", "food@example.com");
        String token = tokenFromLogin("food@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/foods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Chicken Breast",
                                  "brand":"Generic",
                                  "source":"CUSTOM",
                                  "caloriesPer100g":165,
                                  "proteinPer100g":31,
                                  "carbsPer100g":0,
                                  "fatsPer100g":3.6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chicken Breast"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chicken Breast"));

        mockMvc.perform(put("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Chicken Breast Updated",
                                  "brand":"Premium",
                                  "source":"CUSTOM",
                                  "caloriesPer100g":170,
                                  "proteinPer100g":32,
                                  "carbsPer100g":0,
                                  "fatsPer100g":4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chicken Breast Updated"));

        mockMvc.perform(delete("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAndSearchShouldSupportPagination() throws Exception {
        signUp("Food User", "food-page@example.com");
        String token = tokenFromLogin("food-page@example.com", "secret123");

        createFood(token, "Apple");
        createFood(token, "Banana");
        createFood(token, "Carrot");

        mockMvc.perform(get("/api/foods")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/foods")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Banana"));
    }

    @Test
    void shouldScopeCustomFoodPerUser() throws Exception {
        signUp("Food User One", "food1@example.com");
        signUp("Food User Two", "food2@example.com");
        String token1 = tokenFromLogin("food1@example.com", "secret123");
        String token2 = tokenFromLogin("food2@example.com", "secret123");

        createFood(token1, "Private One");

        mockMvc.perform(get("/api/foods")
                        .header("Authorization", "Bearer " + token2)
                        .param("query", "Private"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldValidateInputAndRequireAuth() throws Exception {
        mockMvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"No Auth",
                                  "caloriesPer100g":100,
                                  "proteinPer100g":10,
                                  "carbsPer100g":10,
                                  "fatsPer100g":5
                                }
                                """))
                .andExpect(status().isUnauthorized());

        signUp("Food User", "food-val@example.com");
        String token = tokenFromLogin("food-val@example.com", "secret123");

        mockMvc.perform(post("/api/foods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"",
                                  "caloriesPer100g":-1,
                                  "proteinPer100g":10,
                                  "carbsPer100g":10,
                                  "fatsPer100g":5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void shouldDefaultAndTrimOptionalFieldsAndRejectCrossUserUpdates() throws Exception {
        signUp("Food Owner", "food-owner@example.com");
        signUp("Food Other", "food-other@example.com");
        String ownerToken = tokenFromLogin("food-owner@example.com", "secret123");
        String otherToken = tokenFromLogin("food-other@example.com", "secret123");

        MvcResult create = mockMvc.perform(post("/api/foods")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"  Oats  ",
                                  "brand":"   ",
                                  "externalId":"  ext-1  ",
                                  "servingSize":40,
                                  "servingUnit":"  g  ",
                                  "caloriesPer100g":389,
                                  "proteinPer100g":16.9,
                                  "carbsPer100g":66.3,
                                  "fatsPer100g":6.9
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Oats"))
                .andExpect(jsonPath("$.brand").doesNotExist())
                .andExpect(jsonPath("$.externalId").value("ext-1"))
                .andExpect(jsonPath("$.servingUnit").value("g"))
                .andExpect(jsonPath("$.source").value("CUSTOM"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Other Update",
                                  "caloriesPer100g":100,
                                  "proteinPer100g":10,
                                  "carbsPer100g":10,
                                  "fatsPer100g":10
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/foods/{id}", id)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    private void createFood(String token, String name) throws Exception {
        mockMvc.perform(post("/api/foods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "source":"CUSTOM",
                                  "caloriesPer100g":100,
                                  "proteinPer100g":10,
                                  "carbsPer100g":10,
                                  "fatsPer100g":5
                                }
                                """.formatted(name)))
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
