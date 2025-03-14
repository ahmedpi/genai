package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.BookRequest;
import com.epam.training.gen.ai.service.OpenAIService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summarizer")
public class SummaryController {

  @Autowired
  private OpenAIService openAIService;

  @PostMapping
  public ResponseEntity<String> searchBook(@RequestBody Map<String,String> request) {
    String inputText = request.get("text");
    System.out.println("Summarizing text: " + inputText);
    return ResponseEntity.ok(openAIService.summarizeText(inputText));
  }
}