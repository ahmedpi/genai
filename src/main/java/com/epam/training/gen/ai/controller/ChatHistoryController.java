package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.OpenAIRequest;
import com.epam.training.gen.ai.service.ChatService;
import com.epam.training.gen.ai.service.DynamicModelChatService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/task-2")
public class ChatHistoryController {

  private final ChatService chatService;
  
  @GetMapping(path = "/chat-history")
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
