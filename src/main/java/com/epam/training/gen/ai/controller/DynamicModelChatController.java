package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.OpenAIRequest;
import com.epam.training.gen.ai.service.ChatService;
import com.epam.training.gen.ai.service.DynamicModelChatService;
import java.io.IOException;
import java.util.List;
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
@RequestMapping("/task-3")
public class DynamicModelChatController {

  private final DynamicModelChatService dynamicModelChatService;

  @GetMapping(path = "/deployments")
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

  @GetMapping(path = "/chat")
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

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.trim().isEmpty()) {
      throw new IllegalArgumentException("Prompt cannot be empty.");
    }
  }
}
