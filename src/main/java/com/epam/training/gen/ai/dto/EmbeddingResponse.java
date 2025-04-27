package com.epam.training.gen.ai.dto;

import com.azure.ai.openai.models.EmbeddingItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

  private List<EmbeddingItem> embeddings;
}
