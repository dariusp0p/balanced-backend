package com.example.balancedbackend.chat.service;

import com.example.balancedbackend.chat.model.ChatMessage;
import com.example.balancedbackend.chat.store.ChatMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ChatSqlToMongoMigration {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    @Bean
    @ConditionalOnProperty(name = "app.chat.sql-migration.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner migrateLegacyChatMessages(
            JdbcTemplate jdbcTemplate,
            ChatMessageRepository chatMessageRepository,
            ObjectMapper objectMapper
    ) {
        return args -> {
            if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT to_regclass('public.chat_messages') IS NOT NULL",
                    Boolean.class
            ))) {
                return;
            }

            List<ChatMessage> legacyMessages = jdbcTemplate.query(
                    """
                    SELECT id, conversation_key, sender_id, receiver_id, content, metadata_json, created_at
                    FROM chat_messages
                    ORDER BY created_at ASC
                    """,
                    (rs, rowNum) -> {
                        ChatMessage message = new ChatMessage();
                        message.setLegacySqlId(rs.getLong("id"));
                        message.setConversationKey(rs.getString("conversation_key"));
                        message.setSenderId(rs.getLong("sender_id"));
                        message.setReceiverId(rs.getLong("receiver_id"));
                        message.setContent(rs.getString("content"));
                        message.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                        message.setMetadata(parseMetadata(objectMapper, rs.getString("metadata_json")));
                        return message;
                    }
            );

            for (ChatMessage message : legacyMessages) {
                if (chatMessageRepository.existsByLegacySqlId(message.getLegacySqlId())) {
                    continue;
                }
                chatMessageRepository.save(message);
            }
        };
    }

    private static Map<String, Object> parseMetadata(ObjectMapper objectMapper, String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (IOException exception) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("legacyMetadataJson", metadataJson);
            fallback.put("migrationWarning", "Metadata could not be parsed as JSON");
            fallback.put("migratedAt", Instant.now().toString());
            return fallback;
        }
    }
}
