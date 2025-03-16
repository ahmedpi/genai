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
    System.out.println("Received prompt: " + prompt);
    String response = chatService.getChatCompletions(prompt);
    return ResponseEntity.ok(response);
  }
}
