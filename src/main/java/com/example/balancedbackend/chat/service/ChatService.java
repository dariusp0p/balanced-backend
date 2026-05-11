package com.example.balancedbackend.chat.service;

import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.UserRepository;
import com.example.balancedbackend.chat.api.dto.ChatMessageRequest;
import com.example.balancedbackend.chat.api.dto.ChatMessageResponse;
import com.example.balancedbackend.chat.model.ChatMessage;
import com.example.balancedbackend.chat.store.ChatMessageRepository;
import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public List<ChatMessageResponse> getConversation(long userId, long otherUserId) {
        ensureUserExists(otherUserId);
        return chatMessageRepository.findTop100ByConversationKeyOrderByCreatedAtAsc(conversationKey(userId, otherUserId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChatMessageResponse send(long senderId, ChatMessageRequest request) {
        if (request.receiverId() == senderId) {
            throw new BadRequestException("Cannot chat with yourself");
        }
        ensureUserExists(request.receiverId());

        ChatMessage message = new ChatMessage();
        message.setConversationKey(conversationKey(senderId, request.receiverId()));
        message.setSenderId(senderId);
        message.setReceiverId(request.receiverId());
        message.setContent(request.content().trim());
        message.setCreatedAt(Instant.now());

        ChatMessage saved = chatMessageRepository.save(message);
        return toResponse(saved);
    }

    public static String conversationKey(long userA, long userB) {
        long low = Math.min(userA, userB);
        long high = Math.max(userA, userB);
        return low + ":" + high;
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderId(),
                sender == null ? "Unknown user" : sender.getName(),
                message.getReceiverId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
