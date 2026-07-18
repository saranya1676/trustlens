package com.truthlens.controller;

import com.truthlens.dto.ApiResponse;
import com.truthlens.dto.ChatRequestDTO;
import com.truthlens.dto.ChatResponseDTO;
import com.truthlens.service.ChatbotService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the TruthLens AI chatbot.
 * Base path: /api/chat
 */
@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * POST /api/chat
     * Accepts a user message and returns an AI-generated response
     * with related articles.
     *
     * Request body:
     * { "message": "What happened in sports recently?", "sessionId": "abc123" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @RequestBody ChatRequestDTO request) {

        String message = StringUtils.trimToEmpty(request.getMessage());
        if (message.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Message cannot be empty"));
        }
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }

        ChatResponseDTO response = chatbotService.chat(message);
        return ResponseEntity.ok(ApiResponse.ok("Chat response", response));
    }

    /**
     * GET /api/chat/status
     * Returns the current knowledge base size — useful for debugging.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> info = Map.of(
            "status",            "ready",
            "knowledgeBaseSize", chatbotService.getKnowledgeBaseSize()
        );
        return ResponseEntity.ok(ApiResponse.ok("Chatbot status", info));
    }

    /**
     * POST /api/chat/refresh
     * Invalidates the in-memory article cache so it is rebuilt on next chat.
     * Call this after importing new articles.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh() {
        chatbotService.invalidateCache();
        return ResponseEntity.ok(ApiResponse.ok("Knowledge base cache cleared. " +
                "It will be rebuilt on the next chat message.", "ok"));
    }
}
