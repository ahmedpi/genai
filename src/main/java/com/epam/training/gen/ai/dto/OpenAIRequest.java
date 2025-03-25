package com.epam.training.gen.ai.dto;

import java.util.List;

public record OpenAIRequest(
    String deploymentName,
    Double temperature,
    Integer maxTokens,
    List<Input> inputs
) {

}
