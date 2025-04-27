package com.epam.training.gen.ai.vector;


import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.textembedding.OpenAITextEmbeddingGenerationService;
import com.microsoft.semantickernel.services.ServiceNotFoundException;
import com.microsoft.semantickernel.services.textembedding.Embedding;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class EmbeddingService {

  public static final int VECTOR_SIZE = 1536;

  private static final String COLLECTION_NAME = "my_collection";
  private final OpenAIAsyncClient openAIAsyncClient;
  private final QdrantClient qdrantClient;
  private final Kernel semanticKernel;

  @Value("${openai-embedding-deployment-name}")
  private String embeddingDeploymentName;

  @Autowired
  public EmbeddingService(OpenAIAsyncClient openAIAsyncClient,
      QdrantClient qdrantClient, @Qualifier("embeddingKernel") Kernel semanticKernel) {
    this.openAIAsyncClient = openAIAsyncClient;
    this.qdrantClient = qdrantClient;
    this.semanticKernel = semanticKernel;
  }

  public List<EmbeddingItem> buildEmbedding(String text) {
    log.info("Building embedding for text: {}", text);
    try {
      EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(List.of(text));

      Mono<Embeddings> embeddingsMono = openAIAsyncClient.getEmbeddings(embeddingDeploymentName,
          embeddingsOptions);

      Embeddings embeddings = embeddingsMono.block();

      if (embeddings == null || embeddings.getData() == null || embeddings.getData().isEmpty()) {
        throw new RuntimeException("No embeddings returned from OpenAI API.");
      }

      return embeddings.getData();

    } catch (Exception e) {
      log.error("Error while generating embedding: {}", e.getMessage());
      throw new RuntimeException("Error while generating embedding", e);
    }
  }

  public String buildAndStoreEmbedding(String text)
      throws ExecutionException, InterruptedException {

    List<EmbeddingItem> embeddings = buildEmbedding(text);
    return saveEmbedding(embeddings);
  }

  public void store(List<Map<String, Object>> input) {
    input.forEach(stringObjectMap -> {
      Map<String, JsonWithInt.Value> payload = generatePayload(stringObjectMap);
      String prompt = getGenericSummarizedString(payload);
      List<Embedding> embeddings = build(prompt);
      List<Points.UpdateResult> response = embeddings.stream().map(embedding -> {
        UUID id = UUID.randomUUID();
        PointStruct point = PointStruct.newBuilder()
            .setId(id(id))
            .setVectors(vectors(embedding.getVector()))
            .putAllPayload(payload)
            .build();

        Points.UpdateResult updateResult;
        try {
          updateResult = qdrantClient.upsertAsync(COLLECTION_NAME, List.of(point)).get();
          log.info("Embedding saved with id {} , status: {}", updateResult.getOperationId(),
              updateResult.getStatus().name());
          return updateResult;
        } catch (Exception e) {
          log.error("Error while storing embedding: {}", e.getMessage());
          throw new RuntimeException("Error while storing embedding", e);
        }
      }).toList();
    });
  }

  private String getGenericSummarizedString(Map<String, JsonWithInt.Value> payload) {
    StringBuilder summary = new StringBuilder();
    // Sort keys to ensure consistent order
    payload.keySet().stream().sorted().forEach(key -> {
      Object value = payload.get(key);
      String displayValue = value.toString().trim();
      // Use "key=value" format for clarity and consistency
      summary.append(key.toLowerCase()).append("=").append(displayValue).append(";");
    });
    String result = summary.toString().trim();
    log.info("Standardized summary string: {}", result);
    return result;
  }

  private Map<String, JsonWithInt.Value> generatePayload(Map<String, Object> input) {
    Map<String, JsonWithInt.Value> payload = new HashMap<>();
    input.forEach((key, value) -> {
      payload.put(key, toValue(value));
    });
    return payload;
  }

  private JsonWithInt.Value toValue(Object obj) {
    JsonWithInt.Value.Builder builder = JsonWithInt.Value.newBuilder();
    if (obj instanceof String str) {
      builder.setStringValue(str);
    } else if (obj instanceof Integer i) {
      builder.setIntegerValue(i);
    } else if (obj instanceof Double d) {
      builder.setDoubleValue(d);
    } else if (obj instanceof Float f) {
      builder.setDoubleValue(f.doubleValue());
    } else if (obj instanceof Boolean b) {
      builder.setBoolValue(b);
    } else if (obj instanceof Long l) {
      builder.setIntegerValue(l.intValue()); // or use setLongValue if available
    } else {
      builder.setStringValue(obj.toString()); // fallback
    }
    return builder.build();
  }

  private static List<PointStruct> getPointStructs(List<EmbeddingItem> embeddings) {
    return embeddings.stream().map(embedding -> {
      UUID id = UUID.randomUUID();
      return PointStruct.newBuilder()
          .setId(id(id))
          .setVectors(vectors(embedding.getEmbedding()))
          .build();
    }).collect(Collectors.toList());
  }

  public List<ScoredPointDto> searchEmbedding(String text) {
    List<ScoredPoint> closestEmbeddings = null;
    try {
      if (!qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()) {
        log.info("Collection doesn't exists: {}", COLLECTION_NAME);
        return java.util.Collections.emptyList();
      }
      var embeddings = retrieveEmbeddings(text);
      var qe = new ArrayList<Float>();
      embeddings.block().getData().forEach(embeddingItem ->
          qe.addAll(embeddingItem.getEmbedding())
      );

      closestEmbeddings = qdrantClient
          .searchAsync(
              SearchPoints.newBuilder()
                  .setCollectionName(COLLECTION_NAME)
                  .addAllVector(qe)
                  .setWithPayload(enable(true))
                  .setLimit(3)
                  .build())
          .get();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }

//    closestEmbeddings.stream().forEach(embedding -> System.out.println(embedding));

    return getSearchResultFromScoredPoint(closestEmbeddings);
  }

  public List<ScoredPointDto> search(String prompt) {
    List<Embedding> embeddings = build(prompt);
    List<Points.ScoredPoint> response = null;
    try {
      response = qdrantClient.searchAsync(Points.SearchPoints.newBuilder()
          .setCollectionName(COLLECTION_NAME)
          .addAllVector(embeddings.getFirst().getVector())
          .setLimit(10)
          .setWithPayload(enable(true))
          .build()).get();
      log.info("completed fetching search result.......");
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    return response.stream().map(scoredPoint -> ScoredPointDto.builder()
        .uuid(scoredPoint.getId().getUuid())
        .payload(scoredPoint.getPayloadMap().entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .score(scoredPoint.getScore())
        .build()).toList();
  }

  public static List<ScoredPointDto> getSearchResultFromScoredPoint(
      List<ScoredPoint> scoredPoints) {
    return scoredPoints.stream()
        .map(scoredPoint -> {
          ScoredPointDto scoredPointDto = new ScoredPointDto();
          scoredPointDto.setUuid(scoredPoint.getId().getUuid());
          scoredPointDto.setScore(scoredPoint.getScore());
          scoredPointDto.setEmbeddingPoints(scoredPoint.getVectors().getVector().getDataList());
          scoredPointDto.setPayload(scoredPoint.getPayloadMap().entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
          return scoredPointDto;
        })
        .collect(Collectors.toList());
  }

  private String saveEmbedding(List<EmbeddingItem> embeddings)
      throws InterruptedException, ExecutionException {

    createCollectionIfNotExists();
    List<PointStruct> pointStructs = getPointStructs(embeddings);

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

  private Mono<Embeddings> retrieveEmbeddings(String text) {
    var qembeddingsOptions = new EmbeddingsOptions(List.of(text));
    return openAIAsyncClient.getEmbeddings(embeddingDeploymentName, qembeddingsOptions);
  }

  public List<Embedding> build(String prompt) {
    OpenAITextEmbeddingGenerationService embeddingGenerationService = null;
    try {
      embeddingGenerationService = semanticKernel.getService(
          OpenAITextEmbeddingGenerationService.class);
    } catch (ServiceNotFoundException e) {
      throw new RuntimeException(e);
    }
    List<Embedding> generatedEmbeddingsAsync = embeddingGenerationService.generateEmbeddingsAsync(
            List.of(prompt))
        .block();

    assert generatedEmbeddingsAsync != null;
    generatedEmbeddingsAsync.forEach(embedding -> {
      log.info("** {}", embedding.getVector());
    });
    return generatedEmbeddingsAsync;
  }
}
