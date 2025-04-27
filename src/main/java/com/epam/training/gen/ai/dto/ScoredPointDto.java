package com.epam.training.gen.ai.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoredPointDto {

  private float score;
  private String uuid;
  private List<Float> embeddingPoints;
}