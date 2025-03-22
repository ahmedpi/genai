package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.service.ChatService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-chat")
public class ChatController {

  @Autowired
  private ChatService chatService;

  @GetMapping(path = "/task-1")
  public ResponseEntity<Map<String, String>> handleChatPrompt(
      @RequestParam("prompt") String prompt) {
    try {
      validatePrompt(prompt);
      System.out.println("Searching for: " + prompt);

      String response = chatService.getChatCompletions(prompt);

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

  @GetMapping(path = "/task-2")
  public ResponseEntity<String> handleChatPromptWithHistory(
      @RequestParam("prompt") String prompt) {
    try {
      validatePrompt(prompt);
      System.out.println("Received prompt: " + prompt);

      String response = chatService.getChatCompletionsWithHistory(prompt);
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
