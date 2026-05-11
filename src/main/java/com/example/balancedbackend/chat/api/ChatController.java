package com.example.balancedbackend.chat.api;

import com.example.balancedbackend.chat.api.dto.ChatMessageRequest;
import com.example.balancedbackend.chat.api.dto.ChatMessageResponse;
import com.example.balancedbackend.chat.service.ChatService;
import com.example.balancedbackend.shared.security.SecuritySupport;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final SecuritySupport securitySupport;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(
            ChatService chatService,
            SecuritySupport securitySupport,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatService = chatService;
        this.securitySupport = securitySupport;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/{otherUserId}")
    public List<ChatMessageResponse> conversation(
            Authentication authentication,
            @PathVariable long otherUserId
    ) {
        return chatService.getConversation(securitySupport.requireUserId(authentication), otherUserId);
    }

    @PostMapping
    public ChatMessageResponse send(
            Authentication authentication,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.send(securitySupport.requireUserId(authentication), request);
        publish(response);
        return response;
    }

    @MessageMapping("/chat.send")
    public void sendRealtime(ChatMessageRequest request, Authentication authentication) {
        ChatMessageResponse response = chatService.send(securitySupport.requireUserId(authentication), request);
        publish(response);
    }

    private void publish(ChatMessageResponse response) {
        messagingTemplate.convertAndSend("/topic/chat/" + response.senderId(), response);
        messagingTemplate.convertAndSend("/topic/chat/" + response.receiverId(), response);
    }
}
