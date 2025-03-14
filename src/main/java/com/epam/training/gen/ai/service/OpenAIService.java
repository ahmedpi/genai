package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {

  @Value("${client.azureopenai.key}")
  private String clientKey;

  @Value("${client.azureopenai.endpoint}")
  private String clientEndpoint;

  @Value("${model}")
  private String model;

  public String getChatCompletions(String prompt) {
    OpenAIAsyncClient client = createOpenAIAsyncClient();

    ChatCompletionService chatCompletionService = createChatCompletionService(client);
    Kernel kernel = createKernel(chatCompletionService);

    ChatHistory history = new ChatHistory();
    history.addUserMessage(prompt);

    List<ChatMessageContent<?>> response = fetchChatResponse(chatCompletionService, history,
        kernel);

    return processResponse(response, history);
  }

  private OpenAIAsyncClient createOpenAIAsyncClient() {
    if (clientKey != null) {
      return new OpenAIClientBuilder()
          .credential(new AzureKeyCredential(clientKey))
          .endpoint(clientEndpoint)
          .buildAsyncClient();
    } else {
      return new OpenAIClientBuilder()
          .credential(new KeyCredential(clientKey))
          .buildAsyncClient();
    }
  }

  private ChatCompletionService createChatCompletionService(OpenAIAsyncClient client) {
    return OpenAIChatCompletion.builder()
        .withOpenAIAsyncClient(client)
        .withModelId(model)
        .build();
  }

  private Kernel createKernel(ChatCompletionService chatCompletionService) {
    return Kernel.builder()
        .withAIService(ChatCompletionService.class, chatCompletionService)
        .build();
  }

  private List<ChatMessageContent<?>> fetchChatResponse(
      ChatCompletionService chatCompletionService,
      ChatHistory history,
      Kernel kernel
  ) {
    InvocationContext optionalInvocationContext = null;
    return chatCompletionService.getChatMessageContentsAsync(history, kernel,
        optionalInvocationContext).block();
  }

  private String processResponse(List<ChatMessageContent<?>> response, ChatHistory history) {
    if (response == null || response.isEmpty()) {
      return "No response received from the assistant.";
    }

    for (ChatMessageContent<?> result : response) {
      if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
        System.out.println("Assistant > " + result);
        history.addMessage(result); // Add assistant's message to chat history
      }
    }
    return response.get(0).getContent();
    // Return the first assistant message as the response
//    return new BookResponse(response.get(0).getContent());
  }
}
