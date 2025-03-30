package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.epam.training.gen.ai.dto.Input;
import com.epam.training.gen.ai.dto.ModelInfo;
import com.epam.training.gen.ai.dto.ModelListResponse;
import com.epam.training.gen.ai.dto.OpenAIRequest;
import com.epam.training.gen.ai.util.ChatUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import java.net.http.HttpClient;
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

  private static final String DEPLOYMENTS_API_URL = "https://ai-proxy.lab.epam.com/openai/deployments";
  private static final String NO_RESPONSE_FOUND_MSG = "No response received from the assistant.";

  //  private static final String DEFAULT_SYSTEM_MSG = """
  //          You will be provided with statements probably with grammatical and vocabulary mistakes,
  //          and your task is to convert them to standard English
  //          Be polite and give brief explanation of the correction.
  //      """;

  @Value("${client.azureopenai.key}")
  private String key;

  @Value("${client.azureopenai.endpoint}")
  private String endpoint;

  @Value("${client.azureopenai.deployment-name}")
  private String deploymentName;

  @Autowired
  private OpenAIAsyncClient openAIAsyncClient;

  @Autowired
  private ChatCompletionService defaultChatCompletionService;

  private final HttpClient httpClient = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final RestTemplate restTemplate;

  private final ChatHistory chatHistory = new ChatHistory();

  public DynamicModelChatService() {
    this.restTemplate = new RestTemplate();
//    chatHistory.addSystemMessage(DEFAULT_SYSTEM_MSG);
  }

  /**
   * Retrieves the list of available deployment models.
   *
   * @return List of deployment model names
   */
  public List<String> getAvailableDeployments() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Api-Key", key);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<ModelListResponse> response = restTemplate.exchange(
        DEPLOYMENTS_API_URL,
        HttpMethod.GET,
        entity,
        ModelListResponse.class
    );

    return response.getBody().getData().stream()
        .map(ModelInfo::getModel)
        .toList();
  }

  /**
   * Handles dynamic model selection for chat completions and returns the assistant's response.
   *
   * @param request OpenAIRequest containing input data for the chat
   * @return Assistant's response
   */
  public String getChatCompletionsDifferentModels(OpenAIRequest request) {
    String deploymentName = getDeploymentName(request);
    ChatCompletionService chatCompletionService = createChatCompletionService(deploymentName);

    InvocationContext invocationContext = createInvocationContext(request, deploymentName);
    Kernel semanticKernel = createSemanticKernel(chatCompletionService);

    log.info("Created ChatCompletionService for deployment: {}",
        chatCompletionService.getModelId());

    populateChatHistory(request.inputs());

    List<ChatMessageContent<?>> responses = getResponses(chatCompletionService, semanticKernel,
        invocationContext);

    if (responses == null || responses.isEmpty()) {
      log.warn("No response received for chat completions.");
      return NO_RESPONSE_FOUND_MSG;
    }

    addAssistantMessageToChatHistory(responses.get(0).getContent());

    ChatUtils.printChatHistory(chatHistory);

    return responses.get(0).getContent();
  }

  private String getDeploymentName(OpenAIRequest request) {
    if (request.deploymentName() != null && !request.deploymentName().isEmpty()) {
      log.info("Using dynamic deployment: {}", request.deploymentName());
      return request.deploymentName();
    }

    log.info("Using default deployment: {}", deploymentName);
    return deploymentName;
  }

  private ChatCompletionService createChatCompletionService(String deployment) {
    if (deployment.equals(deploymentName)) {
      return defaultChatCompletionService;
    }

    return OpenAIChatCompletion.builder()
        .withModelId(deploymentName)
        .withOpenAIAsyncClient(openAIAsyncClient)
        .build();
  }

  private InvocationContext createInvocationContext(OpenAIRequest request, String deploymentName) {
    return InvocationContext.builder()
        .withPromptExecutionSettings(
            ChatUtils.buildPromptSettings(deploymentName, request.maxTokens(),
                request.temperature()))
        .build();
  }

  private Kernel createSemanticKernel(ChatCompletionService chatCompletionService) {
    return Kernel.builder()
        .withAIService(ChatCompletionService.class, chatCompletionService)
        .build();
  }

  private void populateChatHistory(List<Input> inputs) {
    inputs.forEach(input -> {
      log.info("Processing input -> Role: {}, Text: {}", input.getRole(), input.getText());
      if ("system".equalsIgnoreCase(input.getRole())) {
        chatHistory.addSystemMessage(input.getText());
      } else {
        chatHistory.addUserMessage(input.getText());
      }
    });
  }

  private List<ChatMessageContent<?>> getResponses(ChatCompletionService chatCompletionService,
      Kernel kernel, InvocationContext context) {
    log.info("Fetching responses from chat completion service...");
    return chatCompletionService.getChatMessageContentsAsync(chatHistory, kernel, context).block();
  }

  private void addAssistantMessageToChatHistory(String content) {
    chatHistory.addAssistantMessage(content);
  }

  private OpenAIClient createOpenAIClient() {
    return new OpenAIClientBuilder()
        .credential(new AzureKeyCredential(key))
        .endpoint(endpoint)
        .buildClient();
  }
}
