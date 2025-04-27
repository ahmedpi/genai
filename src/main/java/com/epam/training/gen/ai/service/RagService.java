package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.JsonWithInt.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    log.info("Received a request to upload a knowledge source from file");
    String content = parseFileContent(file);

    Map<String, Value> payload = new HashMap<>();
    payload.put("Context", JsonWithInt.Value.newBuilder().setStringValue(content).build());

    String status = embeddingService.buildAndStoreEmbedding(content, payload);

    log.info("Knowledge source from file uploaded and processed, status: {} ", status);
  }

  public void storeKnowledgeSource(String url) throws ExecutionException, InterruptedException {
    log.info("Received a request to upload a knowledge source from URL");
    String content = null;
    try {
      Document doc = Jsoup.connect(url).get();
      content = doc.text();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Map<String, Value> payload = new HashMap<>();
    payload.put("Context", JsonWithInt.Value.newBuilder().setStringValue(content).build());

    String status = embeddingService.buildAndStoreEmbedding(content, payload);

    log.info("Knowledge source from URL uploaded and processed, status: {} ", status);
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
        "You are a helpful assistant. "
            + "Use the following knowledge source to answer the question.%n%n"
            + "Knowledge Source:%n%s%n%n"
            + "Question: %s. "
            + "if you can't find answer from the source, mention it and proceed with answer",
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
    InputStream fileStream = file.getInputStream();

    if (filename.endsWith(".txt")) {
      return new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
    } else if (filename.endsWith(".pdf")) {
      return extractTextFromPdf(fileStream);
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + filename);
    }
  }

  private String extractTextFromPdf(InputStream fileStream) throws IOException {
    try (PDDocument document = PDDocument.load(fileStream)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    }
  }
}
