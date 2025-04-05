package com.epam.training.gen.ai.dto;

public record Request(
    String deploymentName,
    Double temperature,
    String prompt
) {

}
