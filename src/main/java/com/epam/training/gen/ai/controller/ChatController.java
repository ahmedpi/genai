package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping
    public ResponseEntity<String> handleChatPrompt(
            @RequestParam("prompt") String prompt) {
        try {
            validatePrompt(prompt);
            System.out.println("Received prompt: " + prompt);

            String response = chatService.getChatCompletions(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String errorMessage = "Error processing request: " + e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty.");
        }
    }
}
