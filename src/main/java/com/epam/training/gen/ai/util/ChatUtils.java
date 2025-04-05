package com.epam.training.gen.ai.util;

import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;

public class ChatUtils {

  /**
   * Builds and returns a PromptExecutionSettings instance with predefined settings.
   *
   * @return a configured PromptExecutionSettings instance
   */
  public static PromptExecutionSettings buildPromptSettings(String deploymentName, Double temperature) {
    return PromptExecutionSettings.builder()
        .withModelId(deploymentName)
        .withTemperature(null == temperature ? 0.5 : temperature) // higher value, more creative
        .withTopP(0.9) // higher value, more diverse
        .withFrequencyPenalty(0.3) // higher value, less repetitive
        .withPresencePenalty(0.3) // higher value, less repetitive
        .build();
  }

  public static void printChatHistory(ChatHistory chatHistory) {
    System.out.println("Chat History:");
    chatHistory.forEach(chatMessageContent -> {
      String role = chatMessageContent.getAuthorRole().toString().toLowerCase();
      String content = chatMessageContent.getContent();
      System.out.printf("%s: %s%n", capitalize(role), content);
    });
  }

  private static String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
}
