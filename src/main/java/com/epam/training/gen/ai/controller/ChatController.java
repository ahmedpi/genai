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
@RequestMapping("/task-1")
public class ChatController {

  private final ChatService chatService;

  private final DynamicModelChatService dynamicModelChatService;

  @GetMapping(path = "/chat")
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

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.trim().isEmpty()) {
      throw new IllegalArgumentException("Prompt cannot be empty.");
    }
  }
}
