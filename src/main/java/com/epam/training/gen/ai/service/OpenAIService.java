package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.epam.training.gen.ai.dto.BookResponse;
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

  public BookResponse processBookPrompt(String prompt) {
    //Uncomment to use change message
//    String prompt = """
//        I want to find top-10 books about world history
//         """;
    ChatHistory history = new ChatHistory();
    history.addUserMessage(prompt);
    List<ChatMessageContent<?>> response = getCompletion(prompt, history);

    return processBookResponse(response, history);
  }

  public String summarizeText(String text) {
    //Uncomment to change user message
//    text = """
//        Making a cup of tea is easy! First, you need to get some\nwater boiling. While that's happening\\ngrab a cup and put a tea bag in it. Once the water is\nhot enough, just pour it over the tea bag.\\nLet it sit for a bit so the tea can steep. After a\nfew minutes, take out the tea bag. If you \nlike, you can add some sugar or milk to taste.\nAnd that's it! You've got yourself a delicious \ncup of tea to enjoy
//        """;

    String prompt = """
        You will be provided with text delimited by triple quotes. 
        If it contains a sequence of instructions, 
        re-write those instructions in the following format:

        Step 1 - ...
        Step 2 - …
        …
        Step N - …

        If the text does not contain a sequence of instructions, 
        then simply write "No steps provided."

        \"\"\"""" + text + "\"\"\"";
    ChatHistory history = new ChatHistory();
    history.addUserMessage(prompt);
    List<ChatMessageContent<?>> response = getCompletion(prompt, history);

    return processResponse(response, history);
  }

  private List<ChatMessageContent<?>> getCompletion(String prompt, ChatHistory history) {
    OpenAIAsyncClient client = createOpenAIAsyncClient();

    ChatCompletionService chatCompletionService = createChatCompletionService(client);
    Kernel kernel = createKernel(chatCompletionService);

    return fetchChatResponse(chatCompletionService, history,
        kernel);
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

  private BookResponse processBookResponse(List<ChatMessageContent<?>> response,
      ChatHistory history) {
    if (response == null || response.isEmpty()) {
      return new BookResponse("No response received from the assistant.");
    }

    for (ChatMessageContent<?> result : response) {
      if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
        System.out.println("Assistant > " + result);
        history.addMessage(result); // Add assistant's message to chat history
      }
    }

    // Return the first assistant message as the response
    return new BookResponse(response.get(0).getContent());
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

    // Return the first assistant message as the response
    return response.get(0).getContent();
  }
}
