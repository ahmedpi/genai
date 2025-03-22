package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ChatService {

  private final ChatCompletionService chatCompletionService;
  private final InvocationContext invocationContext;
  private final Kernel semanticKernel;
  private static final String NO_RESPONSE_FOUND_MSG = "No response received from the assistant.";

  ChatHistory chatHistory = new ChatHistory();

  public ChatService(ChatCompletionService chatCompletionService,
      InvocationContext invocationContext, Kernel semanticKernel) {
    this.chatCompletionService = chatCompletionService;
    this.invocationContext = invocationContext;
    this.semanticKernel = semanticKernel;
    this.chatHistory.addSystemMessage("""
        You are a helpful assistant.
        Your task is to assist with order requests for our menu.
        We have pizza, pasta, and salad available to order.
        """);
  }

  public String getChatCompletions(String prompt) {
    ChatHistory history = new ChatHistory();
    history.addMessage(AuthorRole.USER, prompt);

    List<ChatMessageContent<?>> responses = chatCompletionService.getChatMessageContentsAsync(
        history, semanticKernel,
        null).block();

    return (responses != null && !responses.isEmpty()) ? responses.get(0).getContent()
        : NO_RESPONSE_FOUND_MSG;
  }

  public String getChatCompletionsWithHistory(String prompt) {
    chatHistory.addUserMessage(prompt);
    List<ChatMessageContent<?>> responses = chatCompletionService.getChatMessageContentsAsync(
        chatHistory,
        semanticKernel,
        invocationContext
    ).block();

    if (responses == null || responses.isEmpty()) {
      return NO_RESPONSE_FOUND_MSG;
    }

    chatHistory.addAssistantMessage(responses.get(0).getContent());

    printChatHistory();

    return responses.get(0).getContent();
  }

  public void printChatHistory() {
    System.out.println("Chat History:");
    chatHistory.forEach(chatMessageContent -> {
      String role = chatMessageContent.getAuthorRole().toString().toLowerCase();
      String content = chatMessageContent.getContent();
      System.out.printf("%s: %s%n", capitalize(role), content);
    });
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }
}
