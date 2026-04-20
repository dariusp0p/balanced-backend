package com.example.balancedbackend.auth.store;

import com.example.balancedbackend.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryUserStore {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userIdsByEmail = new ConcurrentHashMap<>();

    public Optional<User> findByEmail(String email) {
        Long userId = userIdsByEmail.get(normalizeEmail(email));
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<User> findById(long userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    public synchronized User createUser(String name, String email, String passwordHash) {
        String normalizedEmail = normalizeEmail(email);
        if (userIdsByEmail.containsKey(normalizedEmail)) {
            throw new IllegalStateException("Email already exists");
        }

        long userId = idSequence.getAndIncrement();
        User user = new User(userId, name.trim(), normalizedEmail, passwordHash);
        usersById.put(userId, user);
        userIdsByEmail.put(normalizedEmail, userId);
        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

