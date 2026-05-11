package com.example.balancedbackend.chat.store;

import com.example.balancedbackend.chat.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findTop100ByConversationKeyOrderByCreatedAtAsc(String conversationKey);
}
