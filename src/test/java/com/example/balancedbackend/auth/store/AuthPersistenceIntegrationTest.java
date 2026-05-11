package com.example.balancedbackend.auth.store;

import com.example.balancedbackend.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AuthPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void userShouldPersistWithDefaultDailyNutritionTargets() {
        User user = new User("Persist User", "persist@example.com", "hash");
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getDailyCalorieTarget()).isEqualTo(2000);
        assertThat(saved.getDailyProteinTarget()).isEqualTo(150);
        assertThat(saved.getDailyCarbsTarget()).isEqualTo(250);
        assertThat(saved.getDailyFatsTarget()).isEqualTo(70);
    }

    @Test
    void emailShouldBeNormalizedToLowerCaseBeforePersist() {
        User saved = userRepository.saveAndFlush(new User("Normalize User", "MIXED@Example.COM", "hash"));

        assertThat(saved.getEmail()).isEqualTo("mixed@example.com");
    }

    @Test
    void normalizedEmailShouldRemainUniqueInDatabase() {
        userRepository.saveAndFlush(new User("User One", "same@example.com", "hash"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("User Two", "SAME@example.com", "hash"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
