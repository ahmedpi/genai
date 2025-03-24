package com.epam.training.gen.ai.dto;

public record PromptRequest(
    String prompt,
    String deploymentName,
    Double temperature
) {

}
