package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.Request;
import com.epam.training.gen.ai.service.ChatWithPluginsService;
import com.epam.training.gen.ai.service.DynamicModelChatService;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/task-4")
public class ChatWithPluginsController {

  private final ChatWithPluginsService chatWithPluginsService;

  private final DynamicModelChatService dynamicModelChatService;

  @PostMapping(path = "/chat-with-plugins")
  public ResponseEntity<String> handleChatPrompt(
      @RequestBody Request request) {
    try {
      validatePrompt(request.prompt());
      System.out.println("Prompt: " + request.prompt());

      String response = chatWithPluginsService.chatWithPlugins(request);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
    }
  }

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.trim().isEmpty()) {
      throw new IllegalArgumentException("Prompt cannot be empty.");
    }
  }
}
