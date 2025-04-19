package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.dto.PromptRequest;
import com.epam.training.gen.ai.util.ChatUtils;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatWithPluginsService {

  private final ChatCompletionService chatCompletionService;
  private final InvocationContext invocationContext;
  private final Kernel semanticKernel;
  private static final String NO_RESPONSE_FOUND_MSG = "No response received from the assistant.";

  ChatHistory chatHistory = new ChatHistory();

  public ChatWithPluginsService(ChatCompletionService chatCompletionService,
      InvocationContext invocationContext, Kernel semanticKernel) {
    this.chatCompletionService = chatCompletionService;
    this.invocationContext = invocationContext;
    this.semanticKernel = semanticKernel;
  }

  public String chatWithPlugins(PromptRequest request) {
    InvocationContext invocationContext = new InvocationContext.Builder()
        .withPromptExecutionSettings(
            ChatUtils.buildPromptSettings(request.deploymentName(),
                request.temperature()))
        .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
        .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
        .build();

    chatHistory.addUserMessage(request.prompt());

    List<ChatMessageContent<?>> results = chatCompletionService
        .getChatMessageContentsAsync(chatHistory, semanticKernel, invocationContext)
        .block();

    if (results == null || results.isEmpty()) {
      log.warn("No response received for chat completions.");
      return NO_RESPONSE_FOUND_MSG;
    }

    System.out.println("Assistant > " + results.get(0));

    chatHistory.addAssistantMessage(results.get(0).getContent());

    ChatUtils.printChatHistory(chatHistory);
    return results.get(0).getContent();
  }
}
