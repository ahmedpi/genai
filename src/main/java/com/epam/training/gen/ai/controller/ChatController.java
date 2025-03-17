package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.service.OpenAIService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private OpenAIService openAIService;

    @GetMapping
    public ResponseEntity<Map<String, String>> handleChatPrompt(
            @RequestParam("prompt") String prompt) {
        try {
            validatePrompt(prompt);
            System.out.println("Searching for: " + prompt);

            String response = openAIService.getChatCompletions(prompt);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("prompt", prompt);
            responseBody.put("response", response);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Error processing request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty.");
        }
    }
}
