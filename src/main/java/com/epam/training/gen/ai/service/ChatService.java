package com.epam.training.gen.ai.service;

import com.epam.training.gen.ai.dto.ChatHistoryDto;
import com.epam.training.gen.ai.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
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
    ChatHistory chatHistory = new ChatHistory();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatService(ChatCompletionService chatCompletionService) {
        this.chatCompletionService = chatCompletionService;
        this.chatHistory.addSystemMessage("""
                You are a helpful assistant.
                Your task is to assist with order requests for our menu.
                We have pizza, pasta, and salad available to order.
                """);
    }

    public String getChatCompletions(String prompt) {
        Kernel kernel = createKernel(chatCompletionService);
        chatHistory.addUserMessage(prompt);

        List<ChatMessageContent<?>> results = chatCompletionService.getChatMessageContentsAsync(
                chatHistory,
                kernel,
                null
        ).block();

        logMessagesHistory();

        return processResponse(results);
    }

    public void logMessagesHistory() {
        try {
            ChatHistoryDto chatHistoryDto = new ChatHistoryDto();

            List<Message> messages = new ArrayList<>();
            chatHistory.forEach(chatMessageContent -> {
                Message message = new Message(
                        chatMessageContent.getAuthorRole().toString().toLowerCase(),
                        chatMessageContent.getContent()
                );
                messages.add(message);
            });

            chatHistoryDto.setMessages(messages);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatHistoryDto);
            log.info("Chat History: {}", json);
        } catch (Exception e) {
            log.error("Failed to serialize chat history to JSON", e);
        }
    }

    private Kernel createKernel(ChatCompletionService chatCompletionService) {
        return Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletionService)
                .build();
    }

    private String processResponse(List<ChatMessageContent<?>> response) {
        if (response == null || response.isEmpty()) {
            return "No response received from the assistant.";
        }

        for (ChatMessageContent<?> result : response) {
            if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
                log.info("Assistant > {}", result);
                chatHistory.addAssistantMessage(result.getContent()); // Add assistant's message to chat history
            }
        }
        return response.get(0).getContent();
    }
}
