package com.epam.training.gen.ai.dto;

public record PromptRequest(
    String deploymentName,
    Double temperature,
    String prompt
) {

}
