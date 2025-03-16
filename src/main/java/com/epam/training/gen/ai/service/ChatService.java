package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.dto.ChatHistoryDto;
import com.epam.training.gen.ai.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChatService {

    private final ChatCompletionService chatCompletionService;
    private final InvocationContext invocationContext;

    ChatHistory chatHistory = new ChatHistory();

    public ChatService(ChatCompletionService chatCompletionService, InvocationContext invocationContext) {
        this.chatCompletionService = chatCompletionService;
        this.invocationContext = invocationContext;
        this.chatHistory.addSystemMessage("""
                You are a helpful assistant.
                Your task is to assist with order requests for our menu.
                We have pizza, pasta, and salad available to order.
                """);
    }

    public String getChatCompletions(String prompt) {
        Kernel kernel = createKernel(chatCompletionService);

        chatHistory.addUserMessage(prompt);
        List<ChatMessageContent<?>> result = chatCompletionService.getChatMessageContentsAsync(
                chatHistory,
                kernel,
                invocationContext
        ).block();

        if (result == null || result.isEmpty()) {
            return "No response received from the assistant.";
        }

        chatHistory.addAssistantMessage(result.get(0).getContent());

        printChatHistory();

        return result.get(0).getContent();
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

    private Kernel createKernel(ChatCompletionService chatCompletionService) {
        return Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletionService)
                .build();
    }
}
