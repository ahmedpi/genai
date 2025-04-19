package com.epam.training.gen.ai.vector;


import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.epam.training.gen.ai.dto.ScoredPointDto;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Value("${openai-embedding-deployment-name}")
  private String embeddingDeploymentName;

  @Autowired
  public EmbeddingService(OpenAIAsyncClient openAIAsyncClient,
      QdrantClient qdrantClient) {
    this.openAIAsyncClient = openAIAsyncClient;
    this.qdrantClient = qdrantClient;
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

  private static List<PointStruct> getPointStructs(List<EmbeddingItem> embeddings) {
    return embeddings.stream().map(embeddingItem -> {
      UUID id = UUID.randomUUID();
      return PointStruct.newBuilder()
          .setId(id(id))
          .setVectors(vectors(embeddingItem.getEmbedding()))
          .build();
    }).collect(Collectors.toList());
  }

  public List<ScoredPointDto> searchEmbedding(String text)
      throws ExecutionException, InterruptedException {
    if (!qdrantClient.collectionExistsAsync(COLLECTION_NAME).get()) {
      log.info("Collection doesn't exists: {}", COLLECTION_NAME);
      return java.util.Collections.emptyList();
    }
    var embeddings = retrieveEmbeddings(text);

    var qe = new ArrayList<Float>();
    embeddings.block().getData().forEach(embeddingItem ->
        qe.addAll(embeddingItem.getEmbedding())
    );
    List<ScoredPoint> closestEmbeddings = qdrantClient
        .searchAsync(
            SearchPoints.newBuilder()
                .setCollectionName(COLLECTION_NAME)
                .addAllVector(qe)
                .setWithPayload(enable(true))
                .setLimit(3)
                .build())
        .get();

//    closestEmbeddings.stream().forEach(embedding -> System.out.println(embedding));

    return getSearchResultFromScoredPoint(closestEmbeddings);
  }

  public static List<ScoredPointDto> getSearchResultFromScoredPoint(
      List<ScoredPoint> scoredPoints) {
    return scoredPoints.stream()
        .map(scoredPoint -> {
          ScoredPointDto scoredPointDto = new ScoredPointDto();
          scoredPointDto.setUuid(scoredPoint.getId().getUuid());
          scoredPointDto.setScore(scoredPoint.getScore());
          scoredPointDto.setEmbeddingPoints(scoredPoint.getVectors().getVector().getDataList());
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
}
