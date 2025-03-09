package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.BookRequest;
import com.epam.training.gen.ai.dto.BookResponse;
import com.epam.training.gen.ai.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

  @Autowired
  private OpenAIService openAIService;

  @PostMapping
  public ResponseEntity<BookResponse> searchBook(@RequestBody BookRequest request) {
    String input = request.getInput();
    System.out.println("Searching for: " + input);
    return ResponseEntity.ok(openAIService.processBookPrompt(input));
  }
}
