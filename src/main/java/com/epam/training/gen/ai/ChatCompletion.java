package com.epam.training.gen.ai;

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
import org.springframework.stereotype.Service;

public class ChatCompletion {

  private static final String CLIENT_KEY =  System.getenv("CLIENT_KEY");
  private static final String AZURE_CLIENT_KEY = System.getenv("AZURE_CLIENT_KEY");

  // Only required if AZURE_CLIENT_KEY is set
  private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");
  private static final String MODEL_ID = System.getenv()
      .getOrDefault("MODEL_ID", "gpt-4o");

  public static void main(String[] args) throws Exception {

    OpenAIAsyncClient client;

    if (AZURE_CLIENT_KEY != null) {
      client = new OpenAIClientBuilder()
          .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
          .endpoint(CLIENT_ENDPOINT)
          .buildAsyncClient();
    } else {
      client = new OpenAIClientBuilder()
          .credential(new KeyCredential(CLIENT_KEY))
          .buildAsyncClient();
    }

    /// Create the chat completion service
    ChatCompletionService openAIChatCompletion = OpenAIChatCompletion.builder()
        .withOpenAIAsyncClient(client)
        .withModelId(MODEL_ID)
        .build();

    // Initialize the kernel
    Kernel kernel = Kernel.builder()
        .withAIService(ChatCompletionService.class, openAIChatCompletion)
        .build();

    ChatHistory history = new ChatHistory();
    history.addUserMessage("whats your name?");

    InvocationContext optionalInvocationContext = null;

    List<ChatMessageContent<?>> response = openAIChatCompletion.getChatMessageContentsAsync(
        history,
        kernel,
        optionalInvocationContext
    ).block();

    for (ChatMessageContent<?> result : response) {
      // Print the results
      if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
        System.out.println("Assistant > " + result);
      }
      // Add the message from the agent to the chat history
      history.addMessage(result);
    }
  }

}
