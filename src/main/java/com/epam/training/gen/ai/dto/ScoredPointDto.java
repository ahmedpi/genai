package com.epam.training.gen.ai.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ScoredPointDto {

  private float score;
  private String uuid;
  private List<Float> embeddingPoints;
  private Map<String, Object> payload;
}