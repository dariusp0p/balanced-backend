package com.example.balancedbackend.audit.api;

import com.example.balancedbackend.audit.model.AppActionLog;
import com.example.balancedbackend.audit.model.ObservedUser;
import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.audit.store.AppActionLogRepository;
import com.example.balancedbackend.audit.store.ObservedUserRepository;
import com.example.balancedbackend.auth.model.Role;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.RoleRepository;
import com.example.balancedbackend.auth.store.UserRepository;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AppActionLogRepository appActionLogRepository;
    @Autowired
    private ObservedUserRepository observedUserRepository;

    @BeforeEach
    void setUp() {
        createRoleIfMissing("ADMIN", "Full permissions");
        createRoleIfMissing("USER", "Restricted permissions");
    }

    @Test
    void adminLogsEndpointShouldRequireAdminAndSupportPaging() throws Exception {
        signUp("Normal User", "audit-user@example.com");
        signUp("Admin User", "audit-admin@example.com");

        User admin = userRepository.findByEmailIgnoreCase("audit-admin@example.com").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);

        String userToken = tokenFromLogin("audit-user@example.com", "secret123");
        String adminToken = tokenFromLogin("audit-admin@example.com", "secret123");

        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Admin access required"));

        auditService.logAction(admin.getId(), "Admin custom action A");
        auditService.logAction(admin.getId(), "Admin custom action B");

        mockMvc.perform(get("/api/admin/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void observedUsersEndpointShouldReturnOnlyActiveAndReuseSingleObservedRow() throws Exception {
        signUp("Admin User", "obs-admin@example.com");
        signUp("Observed User", "obs-user@example.com");

        User admin = userRepository.findByEmailIgnoreCase("obs-admin@example.com").orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        String adminToken = tokenFromLogin("obs-admin@example.com", "secret123");

        Long observedUserId = userRepository.findByEmailIgnoreCase("obs-user@example.com").orElseThrow().getId();
        auditService.observeUser(observedUserId, "First reason");
        auditService.observeUser(observedUserId, "Second reason");

        assertThat(observedUserRepository.findByUserId(observedUserId)).isPresent();
        assertThat(observedUserRepository.findAll().size()).isEqualTo(1);

        ObservedUser inactive = new ObservedUser();
        inactive.setUserId(987654L);
        inactive.setReason("Inactive should be filtered");
        inactive.setObservedAt(Instant.now().minusSeconds(3600));
        inactive.setActive(false);
        observedUserRepository.save(inactive);

        mockMvc.perform(get("/api/admin/observed-users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(observedUserId))
                .andExpect(jsonPath("$[0].reason").value("Second reason"));
    }

    @Test
    void auditServiceShouldMapUnknownUserForLogsAndObservedUsers() {
        auditService.logAction(999_999L, "Unknown actor event");
        auditService.observeUser(888_888L, "Unknown observed reason");

        var logs = auditService.getLogs(0, 100);
        assertThat(logs).anySatisfy(log -> {
            if (log.userId() == 999_999L) {
                assertThat(log.userName()).isEqualTo("Unknown user");
                assertThat(log.userEmail()).isEqualTo("");
                assertThat(log.groupId()).isEqualTo("USER");
                assertThat(log.actionInformation()).isEqualTo("Unknown actor event");
            }
        });

        var observedUsers = auditService.getObservedUsers();
        assertThat(observedUsers).anySatisfy(user -> {
            if (user.userId() == 888_888L) {
                assertThat(user.userName()).isEqualTo("Unknown user");
                assertThat(user.userEmail()).isEqualTo("");
                assertThat(user.reason()).isEqualTo("Unknown observed reason");
            }
        });
    }

    @Test
    void auditEntitiesShouldPreserveExplicitTimestamps() {
        Instant logTime = Instant.parse("2026-05-11T10:15:30Z");
        AppActionLog log = new AppActionLog();
        log.setUserId(123L);
        log.setGroupId("USER");
        log.setActionInformation("Preset timestamp log");
        log.setCreatedAt(logTime);
        AppActionLog savedLog = appActionLogRepository.save(log);
        assertThat(savedLog.getCreatedAt()).isEqualTo(logTime);

        Instant observedTime = Instant.parse("2026-05-11T10:16:30Z");
        ObservedUser observed = new ObservedUser();
        observed.setUserId(124L);
        observed.setReason("Preset timestamp observed");
        observed.setObservedAt(observedTime);
        observed.setActive(true);
        ObservedUser savedObserved = observedUserRepository.save(observed);
        assertThat(savedObserved.getObservedAt()).isEqualTo(observedTime);
    }

    @Test
    void observedUserShouldAutoSetObservedAtWhenMissing() {
        ObservedUser observed = new ObservedUser();
        observed.setUserId(555_555L);
        observed.setReason("Auto timestamp");
        observed.setActive(true);

        ObservedUser saved = observedUserRepository.save(observed);
        assertThat(saved.getObservedAt()).isNotNull();
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
