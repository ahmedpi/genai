package com.epam.training.gen.ai.controller;

import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.service.RagService;
import com.epam.training.gen.ai.vector.EmbeddingService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/rag")
public class RagController {

  private final EmbeddingService embeddingService;
  private final RagService ragService;

  @PostMapping("/source/upload")
  public ResponseEntity<?> uploadKnowledgeSourceFromFile(@RequestParam("file") MultipartFile file) {
    log.info("Received a request to upload a knowledge source.");
    try {
      String content = parseFileContent(file);

      ragService.storeKnowledgeSource(content);

      log.info("Knowledge source uploaded and processed successfully.");
      return ResponseEntity.ok("Knowledge uploaded successfully.");
    } catch (IllegalArgumentException e) {
      log.error("File validation error: {}", e.getMessage());
      return ResponseEntity.badRequest().body("File validation error: " + e.getMessage());
    } catch (Exception e) {
      log.error("An error occurred while uploading the knowledge source: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An internal error occurred: " + e.getMessage());
    }
  }

  @GetMapping("/prompt")
  public ResponseEntity<String> handleChatPrompt(@RequestBody PromptRequest request) {
    log.info("Received a request to get a chat response for the prompt.");
    try {
      String response = ragService.getPromptResponse(request);
      log.info("Successfully retrieved a chat response.");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An internal error occurred while processing the prompt.");
    }
  }

  private String parseFileContent(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
      throw new IllegalArgumentException("File is empty or does not have a valid name.");
    }
    String filename = file.getOriginalFilename();

    if (!filename.endsWith(".txt")) {
      throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    try {
      return new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Error reading file: {}", filename, e);
      throw new IOException("Failed to read file content.", e);
    }
  }
}
