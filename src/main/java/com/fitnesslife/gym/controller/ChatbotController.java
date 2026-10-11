package com.fitnesslife.gym.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesslife.gym.dto.ChatMessageDto;
import com.fitnesslife.gym.dto.ChatRequest;
import com.fitnesslife.gym.dto.ChatResponse;
import com.fitnesslife.gym.service.ChatRateLimiter;
import com.fitnesslife.gym.service.ChatbotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatRateLimiter rateLimiter;

    public ChatbotController(ChatbotService chatbotService, ChatRateLimiter rateLimiter) {
        this.chatbotService = chatbotService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/message")
    public ChatResponse ask(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        String userKey = authentication.getName();
        rateLimiter.check(userKey);
        return chatbotService.ask(userKey, request.message().trim());
    }

    @GetMapping("/history")
    public List<ChatMessageDto> history(Authentication authentication) {
        return chatbotService.history(authentication.getName());
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clear(Authentication authentication) {
        chatbotService.clear(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
