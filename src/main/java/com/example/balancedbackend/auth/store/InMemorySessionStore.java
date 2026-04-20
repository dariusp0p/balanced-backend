package com.example.balancedbackend.auth.store;

import com.example.balancedbackend.auth.model.AuthSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore {

    private final ConcurrentHashMap<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthSession createSession(long userId, Instant expiresAt) {
        String token = UUID.randomUUID().toString();
        AuthSession session = new AuthSession(token, userId, expiresAt);
        sessions.put(token, session);
        return session;
    }

    public Optional<AuthSession> findValidSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        AuthSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }
}

