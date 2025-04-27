package com.epam.training.gen.ai.service;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import com.epam.training.gen.ai.vector.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class RagService {

  public static final int VECTOR_SIZE = 1536;

  private final EmbeddingService embeddingService;
  private static final String COLLECTION_NAME = "my_collection";
  private final OpenAIAsyncClient openAIAsyncClient;
  private final QdrantClient qdrantClient;
  private final ChatService chatService;

  public void storeKnowledgeSource(String knowledge) throws ExecutionException, InterruptedException {
    Map<String, Value> payload = new HashMap<>();
    payload.put("Context", JsonWithInt.Value.newBuilder().setStringValue(knowledge).build());
    List<EmbeddingItem> embeddings = embeddingService.buildEmbedding(knowledge);

    try {
      saveEmbedding(embeddings, payload);
      log.info("Vector saved");
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private String saveEmbedding(List<EmbeddingItem> embeddings, Map<String, Value> payload)
      throws InterruptedException, ExecutionException {

    createCollectionIfNotExists();
    List<PointStruct> pointStructs = getPointStructs(embeddings, payload);

    UpdateResult updateResult;
    try {
      updateResult = qdrantClient.upsertAsync(COLLECTION_NAME, pointStructs).get();
      log.info("saveEmbedding status: {}", updateResult.getStatus().name());
    } catch (Exception e) {
      log.error("Error while storing embedding: {}", e.getMessage());
      throw new RuntimeException("Error while storing embedding", e);
    }
    return updateResult.getStatus().name();
  }

  private static List<PointStruct> getPointStructs(List<EmbeddingItem> embeddings,
      Map<String, Value> payload) {
    return embeddings.stream().map(embedding -> {
      UUID id = UUID.randomUUID();
      return PointStruct.newBuilder()
          .setId(id(id))
          .setVectors(vectors(embedding.getEmbedding()))
          .putAllPayload(payload)
          .build();
    }).collect(Collectors.toList());
  }

  private void createCollectionIfNotExists() throws ExecutionException, InterruptedException {
    if (qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()) {
      log.info("Collection already exists: {}", COLLECTION_NAME);
      return;
    }
    log.info("Creating collection: {}", COLLECTION_NAME);
    Collections.CollectionOperationResponse result = qdrantClient.createCollectionAsync(
            COLLECTION_NAME,
            Collections.VectorParams.newBuilder()
                .setDistance(Collections.Distance.Cosine)
                .setSize(VECTOR_SIZE)
                .build())
        .get();
    log.info("Collection was created: [{}]", result.getResult());
  }

  public String getPromptResponse(PromptRequest promptRequest) {
    List<ScoredPointDto> context = embeddingService.search(
        promptRequest.prompt());
    StringBuilder contextBuilder = new StringBuilder();
    context.forEach(res -> {
      Object data = res.getPayload().get("Context");
      if (data != null) {
        contextBuilder.append(data.toString()).append("\n");
      }
    });

    log.info("context: " + contextBuilder.toString());

    String promptWithContext = String.format(
        "Use the following context to answer the question.%n%n"
            + "Context:%n%s%n%n"
            + "Question: %s",
        contextBuilder,
        promptRequest.prompt()
    );

    return chatService.getChatCompletions(promptWithContext);
  }
}
