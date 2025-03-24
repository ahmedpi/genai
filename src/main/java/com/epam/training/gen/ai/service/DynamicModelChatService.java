package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ImageGenerationData;
import com.azure.ai.openai.models.ImageGenerationOptions;
import com.azure.ai.openai.models.ImageGenerations;
import com.azure.core.credential.AzureKeyCredential;
import com.epam.training.gen.ai.dto.ModelInfo;
import com.epam.training.gen.ai.dto.ModelListResponse;
import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.util.PromptUtil;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class DynamicModelChatService {

  @Autowired
  private OpenAIAsyncClient openAIAsyncClient;

  @Autowired
  private ChatCompletionService defaultChatCompletionService;

  @Value("${client.azureopenai.key}")
  private String key;

  @Value("${client.azureopenai.endpoint}")
  private String endpoint;

  @Value("${client.azureopenai.deployment-name}")
  private String defaultDeployment;

  private RestTemplate restTemplate;

  public DynamicModelChatService() {
    this.restTemplate = new RestTemplate();
  }

  private static final String NO_RESPONSE_FOUND_MSG = "No response received from the assistant.";

  ChatHistory chatHistory = new ChatHistory();

  public String getChatCompletionsDifferentModels(PromptRequest promptRequest) {
    ChatCompletionService chatCompletionService;

    log.info("Fetching conversation response for prompt: {}", promptRequest.prompt());
    String deploymentName;
    if (promptRequest.deploymentName() != null && !promptRequest.deploymentName().isEmpty()) {
      deploymentName = promptRequest.deploymentName();
      chatCompletionService = OpenAIChatCompletion.builder()
          .withModelId(deploymentName)
          .withOpenAIAsyncClient(openAIAsyncClient) // Inject reusable dependency
          .build();
      log.info("Dynamically created ChatCompletionService for deployment: {}", deploymentName);
    } else {
      deploymentName = defaultDeployment;
      chatCompletionService = defaultChatCompletionService;
      log.info("Using default ChatCompletionService bean.");
    }

    InvocationContext invocationContext = InvocationContext.builder()
        .withPromptExecutionSettings(
            PromptUtil.buildPromptSettings(deploymentName, 150,
                promptRequest.temperature()))
        .build();

    chatHistory.addUserMessage(promptRequest.prompt());

    Kernel semanticKernel = Kernel.builder()
        .withAIService(ChatCompletionService.class, chatCompletionService)
        .build();

    List<ChatMessageContent<?>> responses = chatCompletionService.getChatMessageContentsAsync(
        chatHistory,
        semanticKernel,
        invocationContext
    ).block();

    if (responses == null || responses.isEmpty()) {
      return NO_RESPONSE_FOUND_MSG;
    }

    chatHistory.addAssistantMessage(responses.get(0).getContent());

    return responses.get(0).getContent();
  }

  public List<String> getDeployments() {
    String url = "https://ai-proxy.lab.epam.com/openai/deployments"; // Remote API URL

    HttpHeaders headers = new HttpHeaders();
    headers.set("Api-Key", key);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<ModelListResponse> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        ModelListResponse.class
    );

    return response.getBody().getData().stream()
        .map(ModelInfo::getModel)
        .toList();
  }

  public String generateImage(PromptRequest promptRequest) {
    OpenAIClient client = new OpenAIClientBuilder()
        .credential(new AzureKeyCredential(key))
        .endpoint(endpoint)
        .buildClient();

    ImageGenerationOptions imageGenerationOptions = new ImageGenerationOptions(
        "A drawing of the Seattle skyline in the style of Van Gogh");
    ImageGenerations images = client.getImageGenerations(promptRequest.deploymentName(),
        imageGenerationOptions);

    for (ImageGenerationData imageGenerationData : images.getData()) {
      System.out.printf(
          "Image location URL that provides temporary access to download the generated image is %s.%n",
          imageGenerationData.getUrl());
    }

//
//    ImageGenerationOptions imageGenerationOptions = new ImageGenerationOptions(
//        "A drawing of the Seattle skyline in the style of Van Gogh");
//
//    openAIAsyncClient.getImageGenerations(promptRequest.deploymentName(), imageGenerationOptions).subscribe(
//        images -> {
//          for (ImageGenerationData imageGenerationData : images.getData()) {
//            System.out.printf(
//                "Image location URL that provides temporary access to download the generated image is %s.%n",
//                imageGenerationData.getUrl());
//          }
//        },
//        error -> System.err.println("There was an error getting images. " + error),
//        () -> System.out.println("Completed getImages."));

    return "Image Generated";
  }
}
