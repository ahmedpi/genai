package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.JsonWithInt.Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@AllArgsConstructor
public class RagService {

  private final EmbeddingService embeddingService;
  private final ChatService chatService;

  public void storeKnowledgeSource(MultipartFile file)
      throws IOException, ExecutionException, InterruptedException {
    String content = parseFileContent(file);

    Map<String, Value> payload = new HashMap<>();
    payload.put("Context", JsonWithInt.Value.newBuilder().setStringValue(content).build());

    String status = embeddingService.buildAndStoreEmbedding(content, payload);

    log.info("Vector saved. status: {} ", status);
  }

  public String getPromptResponse(PromptRequest promptRequest) {
    List<ScoredPointDto> closestEmbeddings = embeddingService.search(
        promptRequest.prompt());
    StringBuilder contextBuilder = new StringBuilder();
    closestEmbeddings.forEach(res -> {
      Object context = res.getPayload().get("Context");
      if (context != null) {
        contextBuilder.append(context).append("\n");
      }
    });

    log.info("context: {} ", contextBuilder);

    String promptWithContext = String.format(
        "Use the following context to answer the question.%n%n"
            + "Context:%n%s%n%n"
            + "Question: %s",
        contextBuilder,
        promptRequest.prompt()
    );

    return chatService.getChatCompletions(promptWithContext);
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
