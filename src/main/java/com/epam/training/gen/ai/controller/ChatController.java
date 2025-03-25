package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.OpenAIRequest;
import com.epam.training.gen.ai.service.ChatService;
import com.epam.training.gen.ai.service.DynamicModelChatService;
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
@RequestMapping("/ai-chat")
public class ChatController {

  private final ChatService chatService;

  private final DynamicModelChatService dynamicModelChatService;

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

  @GetMapping(path = "/task-3/deployments")
  public ResponseEntity<List<String>> getDeployments() {
    try {
      log.info("Fetching deployments...");
      List<String> deployments = dynamicModelChatService.getAvailableDeployments();
      return ResponseEntity.ok(deployments);
    } catch (Exception e) {
      log.error("Error fetching deployments: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "/task-3")
  public ResponseEntity<String> handleChatPromptDifferentModels(
      @RequestBody OpenAIRequest request) {
    try {
      String response = dynamicModelChatService.getChatCompletionsDifferentModels(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      String errorMessage = "Error processing request: " + e.getMessage();
      return ResponseEntity.badRequest().body(errorMessage);
    }
  }

  @GetMapping(path = "/task-3/generate-image")
  public ResponseEntity<String> generateImage(@RequestBody OpenAIRequest promptRequest) {
    try {
      log.info("Generating image...");
      String image = dynamicModelChatService.generateImage(promptRequest);
      return ResponseEntity.ok(image);
    } catch (Exception e) {
      log.error("Error fetching deployments: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.trim().isEmpty()) {
      throw new IllegalArgumentException("Prompt cannot be empty.");
    }
  }
}
