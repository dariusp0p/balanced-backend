package com.example.balancedbackend.auth.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void ensureRolesExist() {
        createRoleIfMissing("ADMIN", "Full permissions");
        createRoleIfMissing("USER", "Restricted permissions");
    }

    @Test
    void signupShouldCreateUserAndReturnDefaults() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Auth Tester",
                                  "email":"Auth.Tester@example.com",
                                  "password":"secret123",
                                  "confirmPassword":"secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.email").value("auth.tester@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"))
                .andExpect(jsonPath("$.user.dailyNutritionTarget.calories").value(2000.0))
                .andExpect(jsonPath("$.user.dailyNutritionTarget.protein").value(150.0))
                .andExpect(jsonPath("$.user.dailyNutritionTarget.carbs").value(250.0))
                .andExpect(jsonPath("$.user.dailyNutritionTarget.fats").value(70.0));
    }

    @Test
    void signupShouldRejectDuplicateEmailCaseInsensitive() throws Exception {
        signUp("dup@example.com");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Dup",
                                  "email":"DUP@example.com",
                                  "password":"secret123",
                                  "confirmPassword":"secret123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }

    @Test
    void loginShouldReturnTokenAndUserPayload() throws Exception {
        signUp("login@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"login@example.com",
                                  "password":"secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"));
    }

    @Test
    void loginShouldFailForWrongPassword() throws Exception {
        signUp("wrongpass@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"wrongpass@example.com",
                                  "password":"bad-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meShouldReturnAuthenticatedUserWithTargets() throws Exception {
        signUp("me@example.com");
        String token = loginAndGetToken("me@example.com", "secret123");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.dailyNutritionTarget.calories").value(2000.0));
    }

    @Test
    void dailyTargetEndpointsShouldUpdateAndReturnPersistedValues() throws Exception {
        signUp("target@example.com");
        String token = loginAndGetToken("target@example.com", "secret123");

        mockMvc.perform(put("/auth/me/daily-nutrition-target")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "calories": 2400,
                                  "protein": 180,
                                  "carbs": 260,
                                  "fats": 80
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(2400.0))
                .andExpect(jsonPath("$.protein").value(180.0))
                .andExpect(jsonPath("$.carbs").value(260.0))
                .andExpect(jsonPath("$.fats").value(80.0));

        mockMvc.perform(get("/auth/me/daily-nutrition-target")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(2400.0))
                .andExpect(jsonPath("$.protein").value(180.0))
                .andExpect(jsonPath("$.carbs").value(260.0))
                .andExpect(jsonPath("$.fats").value(80.0));
    }

    @Test
    void dailyTargetUpdateShouldRejectNegativeValues() throws Exception {
        signUp("validation@example.com");
        String token = loginAndGetToken("validation@example.com", "secret123");

        mockMvc.perform(put("/auth/me/daily-nutrition-target")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "calories": -1,
                                  "protein": 170,
                                  "carbs": 240,
                                  "fats": 70
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.calories").exists());
    }

    private void signUp(String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Test User",
                                  "email":"%s",
                                  "password":"secret123",
                                  "confirmPassword":"secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
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
        if (roleRepository.findByName(name).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}
