package com.epam.training.gen.ai.controller;

import com.azure.ai.openai.models.EmbeddingItem;
import com.epam.training.gen.ai.dto.EmbeddingRequest;
import com.epam.training.gen.ai.dto.EmbeddingResponse;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import com.epam.training.gen.ai.service.EmbeddingService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/embeddings")
public class EmbeddingController {

  private final EmbeddingService embeddingService;

  @PostMapping("/build")
  public ResponseEntity<?> buildEmbedding(@RequestBody EmbeddingRequest request) {
    try {
      validateInputText(request.getText());

      List<EmbeddingItem> embeddings = embeddingService.buildEmbedding(request.getText());
      return ResponseEntity.ok(new EmbeddingResponse(embeddings));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred: {} " + e.getMessage());
    }
  }

  @PostMapping(path = "/build-and-store")
  public ResponseEntity<String> buildAndStoreEmbedding(
      @RequestBody EmbeddingRequest request) {
    try {
      validateInputText(request.getText());
      System.out.println("Text: {}" + request.getText());

      String status = embeddingService.buildAndStoreEmbedding(request.getText(), null);
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred: " + e.getMessage());
    }
  }

  @PutMapping(value = "/build-and-store-with-payload")
  public ResponseEntity<String> buildAndStoreEmbedding(
      @RequestBody List<Map<String, Object>> input) {
    embeddingService.buildAndStoreEmbeddingWithPayload(input);
    return ResponseEntity.ok("Successfully saved vectors");
  }

  @PostMapping(path = "/search")
  public ResponseEntity<List<ScoredPointDto>> searchClosestEmbedding(
      @RequestBody EmbeddingRequest request) {
    try {
      validateInputText(request.getText());
      System.out.println("Text: " + request.getText());

      List<ScoredPointDto> closestEmbeddings = embeddingService.searchEmbedding(request.getText());
      return ResponseEntity.ok(closestEmbeddings);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.emptyList());
    }
  }

  private void validateInputText(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("Text cannot be empty.");
    }
  }
}
