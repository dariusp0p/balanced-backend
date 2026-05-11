package com.example.balancedbackend.chat.api;

import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.shared.security.AuthenticatedUser;
import com.example.balancedbackend.chat.store.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class ChatControllerIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl("balanced_chat_test"));
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        createRoleIfMissing("ADMIN", "Full permissions");
        createRoleIfMissing("USER", "Restricted permissions");
    }

    @Test
    void sendAndConversationShouldPersistAndReturnMessages() throws Exception {
        signUp("Sender", "sender@example.com");
        signUp("Receiver", "receiver@example.com");

        long receiverId = userIdFromLogin("receiver@example.com", "secret123");
        String senderToken = tokenFromLogin("sender@example.com", "secret123");
        String receiverToken = tokenFromLogin("receiver@example.com", "secret123");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": %d,
                                  "content": "  hello receiver  "
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.content").value("hello receiver"))
                .andExpect(jsonPath("$.senderName").value("Sender"));

        assertThat(chatMessageRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/chat/{otherUserId}", receiverId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("hello receiver"));

        long senderId = userIdFromLogin("sender@example.com", "secret123");
        mockMvc.perform(get("/api/chat/{otherUserId}", senderId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("hello receiver"));
    }

    @Test
    void sendShouldRejectSelfChat() throws Exception {
        signUp("Solo", "solo@example.com");
        String token = tokenFromLogin("solo@example.com", "secret123");
        long userId = userIdFromLogin("solo@example.com", "secret123");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": %d,
                                  "content": "hello"
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot chat with yourself"));
    }

    @Test
    void sendShouldReturnNotFoundForUnknownReceiver() throws Exception {
        signUp("Known", "known@example.com");
        String token = tokenFromLogin("known@example.com", "secret123");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": 999999,
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void conversationShouldReturnNotFoundForUnknownUser() throws Exception {
        signUp("Known", "known2@example.com");
        String token = tokenFromLogin("known2@example.com", "secret123");

        mockMvc.perform(get("/api/chat/{otherUserId}", 888888)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void sendShouldValidateBlankContent() throws Exception {
        signUp("Sender", "validationsender@example.com");
        signUp("Receiver", "validationreceiver@example.com");
        String token = tokenFromLogin("validationsender@example.com", "secret123");
        long receiverId = userIdFromLogin("validationreceiver@example.com", "secret123");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId": %d,
                                  "content": "   "
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void sendRealtimeShouldPersistMessage() throws Exception {
        signUp("Realtime Sender", "realtime-sender@example.com");
        signUp("Realtime Receiver", "realtime-receiver@example.com");

        long senderId = userIdFromLogin("realtime-sender@example.com", "secret123");
        long receiverId = userIdFromLogin("realtime-receiver@example.com", "secret123");

        var principal = new AuthenticatedUser(senderId, "realtime-sender@example.com", "Realtime Sender");
        var authentication = new UsernamePasswordAuthenticationToken(principal, null);

        chatController.sendRealtime(new com.example.balancedbackend.chat.api.dto.ChatMessageRequest(
                receiverId,
                "realtime hello"
        ), authentication);

        assertThat(chatMessageRepository.count()).isEqualTo(1);
        var saved = chatMessageRepository.findAll().getFirst();
        assertThat(saved.getContent()).isEqualTo("realtime hello");
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getReceiverId()).isEqualTo(receiverId);
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
        JsonNode root = loginJson(email, password);
        return root.path("token").asText();
    }

    private long userIdFromLogin(String email, String password) throws Exception {
        JsonNode root = loginJson(email, password);
        return root.path("user").path("id").asLong();
    }

    private JsonNode loginJson(String email, String password) throws Exception {
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
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
