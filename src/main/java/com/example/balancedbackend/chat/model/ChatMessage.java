package com.example.balancedbackend.chat.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(name = "conversation_created_idx", def = "{'conversationKey': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "sender_receiver_idx", def = "{'senderId': 1, 'receiverId': 1}")
})
@Getter
@Setter
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private String conversationKey;

    private Long senderId;

    private Long receiverId;

    private String content;

    private Map<String, Object> metadata = new LinkedHashMap<>();

    private Long legacySqlId;

    private Instant createdAt;
}
